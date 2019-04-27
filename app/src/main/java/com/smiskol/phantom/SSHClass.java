package com.smiskol.phantom;

import android.content.Context;
import android.os.StrictMode;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.io.File;
import java.util.Properties;

public class SSHClass {
    Session session;

    public Boolean closeSession(Session session) {
        try {
            session.disconnect();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public Session getSession(Context context, String eonIP) throws Exception {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        try {
            if (!session.isConnected()) {
                session.connect();
            }
        } catch (Throwable t) {
            JSch jsch = new JSch();
            File file = new File(context.getFilesDir(), "eon_id.ppk");
            jsch.addIdentity(file.getAbsolutePath());
            session = jsch.getSession("root", eonIP, 8022);

            Properties prop = new Properties();
            prop.put("StrictHostKeyChecking", "no");
            prop.put("PreferredAuthentications", "publickey");
            session.setConfig(prop);
            session.connect(2000);
        }
        return session;
    }

    public Boolean sendPhantomCommand(Session session, String eonIP, String enabled, String desiredSpeed, String steeringAngle, String time) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        try {
            ChannelExec channelExec = (ChannelExec) session.openChannel("exec");

            channelExec.setCommand("python /data/openpilot/selfdrive/phantom_receiver.py " + enabled + " " + desiredSpeed + " " + steeringAngle + " " + time);
            channelExec.connect();
            channelExec.disconnect();
            return true;
        } catch (Exception e) {
            System.out.println(eonIP);
            e.printStackTrace();
            return true;
        }
    }
}