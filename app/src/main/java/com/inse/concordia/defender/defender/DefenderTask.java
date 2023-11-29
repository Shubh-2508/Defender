package com.inse.concordia.defender.defender;

import android.content.Context;

public abstract class DefenderTask {

    public int RunEvery=1; //in seconds
    public int Checked=0;
    public abstract void doWork(Context context);
}
