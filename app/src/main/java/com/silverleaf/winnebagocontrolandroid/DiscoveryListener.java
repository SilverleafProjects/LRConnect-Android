package com.silverleaf.winnebagocontrolandroid;

import static java.sql.DriverManager.println;

import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import kotlinx.coroutines.sync.Mutex;

public class DiscoveryListener implements NsdManager.DiscoveryListener {
    final int SLEEP_TIME_MILLISEC = 1200;
    long startTime;

    private List<NsdServiceInfo> serviceList = Collections.synchronizedList(new LinkedList<>());

    public List<NsdServiceInfo> getServiceList() {
        if(serviceList.isEmpty()) {
            try {
                Thread.sleep(SLEEP_TIME_MILLISEC);
            }
            catch(InterruptedException e) {
                // Swallow and ignore.
            }
        }
        return serviceList;
    }

    @Override
    public void onDiscoveryStarted(String s) {
        startTime = System.currentTimeMillis();
        MainActivity.NSDLock = true;
    }

    @Override
    public void onServiceFound(NsdServiceInfo nsdServiceInfo) {
        if(nsdServiceInfo.toString().contains("LR125")) {
            serviceList.add(nsdServiceInfo);
        }
    }

    @Override
    public void onServiceLost(NsdServiceInfo nsdServiceInfo) {

        if(nsdServiceInfo.toString().contains("LR125")) {
            serviceList.remove(nsdServiceInfo);
        }
    }

    @Override
    public void onDiscoveryStopped(String s) {
        MainActivity.NSDLock = false;
    }

    @Override
    public void onStartDiscoveryFailed(String s, int i) {
        MainActivity.NSDLock = false;
    }

    @Override
    public void onStopDiscoveryFailed(String s, int i) {
        MainActivity.NSDLock = false;
    }


}
