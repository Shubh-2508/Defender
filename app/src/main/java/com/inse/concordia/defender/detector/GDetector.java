package com.inse.concordia.defender.detector;

import java.util.Calendar;
import java.util.HashMap;

import android.content.Context;
import android.util.Log;

import com.inse.concordia.defender.defender.DefenderDBHelper;
import com.inse.concordia.defender.defender.DefenderTask;
import com.inse.concordia.defender.analyzer.IEAnalyzer;
import com.inse.concordia.defender.model.APackage;
import com.inse.concordia.defender.model.Alert;
import com.inse.concordia.defender.model.IEModel;

/*
 * Threat detector for IEModels. It compares process activity models to
 * global device model and bumps up package threat when it exceeds it.
 * Since we have an iterative learned model, a process exceeding the
 * global model is suspicious so we directly issue an alert
 */
public class GDetector extends DefenderTask {
    private final String TAG = GDetector.class.getName();

    public GDetector() {
        this.RunEvery = 60;
    }

    public void doWork(Context context) {
        DefenderDBHelper defDBHelper = DefenderDBHelper.getInstance(context);

        Calendar calendar = Calendar.getInstance();
        long currentTimeMillis = calendar.getTimeInMillis();
        Calendar prevCalendar = Calendar.getInstance();
        prevCalendar.add(Calendar.SECOND, -1 * RunEvery);
        long prevTimeMillis = prevCalendar.getTimeInMillis();

        HashMap<String, IEModel> previousProcessesModels = IEAnalyzer
                .getIEModelsForProcesses(defDBHelper, prevTimeMillis,
                        currentTimeMillis);

        defDBHelper.insertLog(String.format("Running GDetector for %s-%s",
                calendar.toString(), prevCalendar.toString()));
        String gModelName = "_global" + calendar.get(Calendar.HOUR_OF_DAY);

        IEModel learnedModel = defDBHelper.getIEModel(gModelName);

        if (learnedModel == null) {
            // model is not old enough or doesn't exist, catch it on next
            // iteration
            return;
        }

        for (String pName : previousProcessesModels.keySet()) {
            IEModel activityModel = previousProcessesModels.get(pName);

            float learnedCPULow = 0;
            float learnedCPUMid = 0;
            float learnedCPUHigh = 0;

            float cpuLow = 0;
            float cpuMid = 0;
            float cpuHigh = 0;

            if (learnedModel.CPUCounter == 0) {
                defDBHelper
                        .insertLog(String
                                .format("Model %s has 0 CPU counter, was comparing activity %s",
                                        learnedModel, activityModel));
                continue;
            }

            if (learnedModel.CPUCounter != 0) {
                learnedCPULow = learnedModel.CPULow / learnedModel.CPUCounter;
                learnedCPUMid = learnedModel.CPUMid / learnedModel.CPUCounter;
                learnedCPUHigh = learnedModel.CPUHigh / learnedModel.CPUCounter;
            }

            // check if activcity model shows activity similar to learned, if
            // not issue alert
            if (activityModel.CPUCounter != 0) {
                cpuLow = activityModel.CPULow / activityModel.CPUCounter;
                cpuMid = activityModel.CPUMid / activityModel.CPUCounter;
                cpuHigh = activityModel.CPUHigh / activityModel.CPUCounter;
            }

            if (cpuLow > learnedCPULow || cpuMid > learnedCPUMid
                    || cpuHigh > learnedCPUHigh
                    || activityModel.RxBytes > learnedModel.RxBytes
                    || activityModel.TxBytes > learnedModel.TxBytes) {
                // process behavior does not follow learned model

                // increase the threat of the package
                APackage pkg = defDBHelper.getPackage(pName);

                // TODO this will happen if there are services in the pkg and
                // each one runs in its own process ex: gmaps
                if (pkg == null) {
                    // independent process, issue alert

                    Alert al = new Alert();
                    al.TimeStamp = Calendar.getInstance().getTimeInMillis();
                    al.Notes = String
                            .format("High threat detected from GDetector for process %s",
                                    pName);
                    defDBHelper.insertAlert(al);

                    defDBHelper
                            .insertLog(String
                                    .format("No PKG. Process %s consumed more resources than global model %s. No Package but acitivty %s",
                                            activityModel.ProcessName,
                                            learnedModel, activityModel));

                    continue;
                }

                pkg.Threat_Numeric += 0.2;
                defDBHelper.updatePackage(pkg);

                Alert al = new Alert();
                al.TimeStamp = Calendar.getInstance().getTimeInMillis();
                al.Notes = String.format(
                        "High threat detected from GDetector for package %s",
                        pkg);
                defDBHelper.insertAlert(al);

                defDBHelper
                        .insertLog(String
                                .format("Process %s consumed more resources than global model %s. Package %s acitivty %s",
                                        activityModel.ProcessName,
                                        learnedModel, pkg, activityModel));
            }
        }
    }
}
