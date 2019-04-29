package com.smiskol.phantom;

import java.io.File;

import android.content.Context;
import android.os.StrictMode;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;

public class SSHClass {
    public Connection getSession(Context context, String eonIP) {
        String username = "root";
        File keyfile = new File(context.getFilesDir(), "eon_id.ppk");
        try {
            Connection conn = new Connection(eonIP, 8022);
            conn.connect();
            boolean isAuthenticated = conn.authenticateWithPublicKey(username, keyfile, "");
            if (!isAuthenticated) {
                return null;
            }
            return conn;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    public Boolean sendPhantomCommand(Connection conn, String enabled, String desiredSpeed, String steeringAngle, String time) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        try {
            Session sess = conn.openSession();
            sess.execCommand("python /data/openpilot/selfdrive/phantom_receiver.py " + enabled + " " + desiredSpeed + " " + steeringAngle + " " + time);
            sess.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
    }
}
