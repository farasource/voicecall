package ghasemi.abbas.voicecall;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.Build;


import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

public class NetworkChangeReceiver extends BroadcastReceiver {
    private NetworkChangeListener networkChangeListener;
    private ConnectivityManager.NetworkCallback networkCallback;

    public NetworkChangeReceiver(){

    }

    public NetworkChangeReceiver(NetworkChangeListener networkChangeListener) {
        if (ApplicationLoader.context == null) {
            return;
        }
        this.networkChangeListener = networkChangeListener;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            registerDefaultNetworkCallback();
        } else {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            ApplicationLoader.context.getApplicationContext().registerReceiver(this, intentFilter);
        }
    }

    public void destroy() {
        try {
            networkChangeListener = null;
            if (ApplicationLoader.context == null) {
                return;
            }
            if (networkCallback == null) {
                ApplicationLoader.context.getApplicationContext().unregisterReceiver(this);
            } else {
                ConnectivityManager cm = (ConnectivityManager) ApplicationLoader.context.getSystemService(Context.CONNECTIVITY_SERVICE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && cm != null && networkCallback != null) {
                    cm.unregisterNetworkCallback(networkCallback);
                }
                networkCallback = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
            if (networkChangeListener != null) {
                networkChangeListener.changed();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void registerDefaultNetworkCallback() {
        ConnectivityManager cm = (ConnectivityManager) ApplicationLoader.context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            cm.registerDefaultNetworkCallback(networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onLost(@NonNull Network network) {
                    super.onLost(network);
                    if (NetworkChangeReceiver.this.networkChangeListener != null) {
                        NetworkChangeReceiver.this.networkChangeListener.changed();
                    }
                }

                @Override
                public void onAvailable(@NonNull Network network) {
                    super.onAvailable(network);
                    if (NetworkChangeReceiver.this.networkChangeListener != null) {
                        NetworkChangeReceiver.this.networkChangeListener.changed();
                    }
                }
            });
        }
    }
}