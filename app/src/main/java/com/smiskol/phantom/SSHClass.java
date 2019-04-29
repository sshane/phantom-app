package com.smiskol.phantom;

import java.io.File;

import android.content.Context;
import android.os.StrictMode;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;

public class SSHClass {
    public Connection getConnection(Context context, String eonIP) {
        String username = "root";
        File keyfile = new File(context.getFilesDir(), "eon_id.ppk");
        try {
            Connection connection = new Connection(eonIP, 8022);
            connection.connect();
            boolean isAuthenticated = connection.authenticateWithPublicKey(username, keyfile, "");
            if (!isAuthenticated) {
                return null;
            }
            return connection;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    public Boolean sendPhantomCommand(Connection conn, String enabled, String desiredSpeed, String steeringAngle, String time) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        try {
            Session session = conn.openSession();
            session.execCommand("python /data/openpilot/selfdrive/phantom_receiver.py " + enabled + " " + desiredSpeed + " " + steeringAngle + " " + time);
            session.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
    }
}
