package com.yunzhanghu.redpacketui;

import android.app.Activity;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by max on 15/9/17
 */
public class AppManager {

    private static AppManager instance = null;

    private static List<Activity> mActivities = new LinkedList<>();

    private AppManager() {

    }

    public synchronized static AppManager getInstance() {
        if (instance == null) {
            instance = new AppManager();
        }
        return instance;
    }

    public int size() {
        return mActivities.size();
    }


    public synchronized void addActivity(Activity activity) {
        mActivities.add(activity);
    }

    public synchronized void removeActivity(Activity activity) {
        if (mActivities.contains(activity)) {
            mActivities.remove(activity);
        }
    }

    public synchronized void clear() {
        for (int i = mActivities.size() - 1; i > -1; i--) {
            Activity activity = mActivities.get(i);
            removeActivity(activity);
            activity.finish();
            i = mActivities.size();
        }
    }

}