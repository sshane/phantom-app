package com.smiskol.phantom;

import android.content.Context;
import android.os.StrictMode;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.io.File;
import java.util.Properties;

public class SSHClass {

    public Boolean testConnection(Context context, String eonIP) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();

        StrictMode.setThreadPolicy(policy);
        try {
            JSch jsch = new JSch();
            File file = new File(context.getFilesDir(), "eon_id.ppk");
            jsch.addIdentity(file.getAbsolutePath());
            Session session = jsch.getSession("root", eonIP, 8022);

            Properties prop = new Properties();
            prop.put("StrictHostKeyChecking", "no");
            prop.put("PreferredAuthentications", "publickey");
            session.setConfig(prop);

            session.connect(5000);

            session.disconnect();
            return true;

        } catch (Exception e) {
            //e.printStackTrace();
            return false;
        }
    }

    public Boolean runSpeedChange(Context context, String eonIP, Integer speedChange) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();

        StrictMode.setThreadPolicy(policy);
        try {
            JSch jsch = new JSch();
            File file = new File(context.getFilesDir(), "eon_id.ppk");
            System.out.println("HERE! "+file.getAbsoluteFile());
            System.out.println("HERE! "+eonIP);
            System.out.println(speedChange.toString());
            jsch.addIdentity(file.getAbsolutePath());
            Session session = jsch.getSession("root", eonIP, 8022);

            Properties prop = new Properties();
            prop.put("StrictHostKeyChecking", "no");
            prop.put("PreferredAuthentications", "publickey");
            session.setConfig(prop);

            session.connect(5000);

            ChannelExec channelssh = (ChannelExec) session.openChannel("exec");

            channelssh.setCommand("python /data/openpilot/selfdrive/speed_controller.py " + speedChange.toString());
            channelssh.connect();
            channelssh.disconnect();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}