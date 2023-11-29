package com.inse.concordia.defender.collector;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.inse.concordia.defender.model.Event;
import com.inse.concordia.defender.defender.DefenderDBHelper;


public class EventCollector extends BroadcastReceiver {
    private static final String TAG = EventCollector.class.getName();

    @Override
    public void onReceive(Context context, Intent intent) {
        DefenderDBHelper defDBHelper = DefenderDBHelper.getInstance(context);
        Event ev = new Event();
        ev.TimeStamp = System.currentTimeMillis();

        if(intent.getAction() == Intent.ACTION_SCREEN_ON){
            ev.Type = Event.Event_Type.SCREEN_ON;
            Log.i("EVENT", "SCREEN ON");
        }
        else if(intent.getAction() == Intent.ACTION_SCREEN_OFF){
            ev.Type = Event.Event_Type.SCREEN_OFF;
            Log.i("EVENT", "SCREEN OFF");
        }

        defDBHelper.insertEvent(ev);
    }
}