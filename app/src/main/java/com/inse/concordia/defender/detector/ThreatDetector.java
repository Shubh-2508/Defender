package com.inse.concordia.defender.detector;

import java.util.Calendar;

import android.content.Context;

import com.inse.concordia.defender.defender.DefenderDBHelper;
import com.inse.concordia.defender.defender.DefenderTask;
import com.inse.concordia.defender.model.APackage;
import com.inse.concordia.defender.model.Alert;

/*
 * This is the watchdog, responsible for iterating over our packages
 * and issuing alerts when we exceed a threshold.
 */
public class ThreatDetector extends DefenderTask {

    private final String TAG = IEDetector.class.getName();

    public ThreatDetector(){
        this.RunEvery = 180;
    }

    @Override
    public void doWork(Context context) {
        DefenderDBHelper defDBHelper = DefenderDBHelper.getInstance(context);

        for(APackage pkg: defDBHelper.getPackage()){

            if(pkg.Threat_Numeric > 0.7){
                //package has high threat
                Alert al = new Alert();
                al.TimeStamp = Calendar.getInstance().getTimeInMillis();
                al.Notes = String.format(
                        "High threat detected for package %s",
                        pkg.Name);
                defDBHelper.insertAlert(al);
            }
        }

    }

}
