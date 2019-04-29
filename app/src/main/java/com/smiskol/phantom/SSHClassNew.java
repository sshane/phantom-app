package com.smiskol.phantom;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.content.Context;
import android.os.StrictMode;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;

public class SSHClassNew {
    Session sess;

    public Boolean getSession(Context context, String eonIP) {
        System.out.println("testhere");
        String username = "root";
        File keyfile = new File(context.getFilesDir(), "eon_id.ppk");
        try {
            /* Create a connection instance */

            Connection conn = new Connection(eonIP, 8022);

            /* Now connect */

            conn.connect();

            /* Authenticate */

            boolean isAuthenticated = conn.authenticateWithPublicKey(username, keyfile, "");

            if (!isAuthenticated) {
                throw new IOException("Authentication failed.");
            }

            sess = conn.openSession();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public Boolean sendPhantomCommand(String enabled, String desiredSpeed, String steeringAngle, String time) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        try {
            sess.execCommand("python /data/openpilot/selfdrive/phantom_receiver.py " + enabled + " " + desiredSpeed + " " + steeringAngle + " " + time);

            InputStream stdout = new ch.ethz.ssh2.StreamGobbler(sess.getStdout());

            BufferedReader br = new BufferedReader(new InputStreamReader(stdout));

            while (true) { // print output
                String line = br.readLine();
                if (line == null)
                    break;
                System.out.println(line);
            }

            /* close buffer Reader */

            br.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
    }

    public void test(Context context) {
        System.out.println("testhere");
        String hostname = "192.168.1.32";
        String username = "root";
        File keyfile = new File(context.getFilesDir(), "eon_id.ppk");
        try {
            /* Create a connection instance */

            Connection conn = new Connection(hostname, 8022);

            /* Now connect */

            conn.connect();

            /* Authenticate */

            boolean isAuthenticated = conn.authenticateWithPublicKey(username, keyfile, "");

            if (!isAuthenticated) {
                throw new IOException("Authentication failed.");
            }

            Session sess = conn.openSession();

            sess.execCommand("cd /data && ls -lah");

            InputStream stdout = new ch.ethz.ssh2.StreamGobbler(sess.getStdout());

            BufferedReader br = new BufferedReader(new InputStreamReader(stdout));

            while (true) {
                String line = br.readLine();
                if (line == null)
                    break;
                System.out.println(line);
            }

            /* close buffer Reader */

            br.close();

            /* Close this session */

            sess.close();

            /* Close the connection */

            conn.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendEONCommand() {
        String hostname = "10.X.X.XX";
        String username = "username";
        String password = "password";

        try {
            // create a connection instance and connect to it
            Connection ssh = new Connection(hostname);

            ssh.connect();
            boolean authorized = ssh.authenticateWithPassword(username,
                    password);
            if (authorized == false)
                throw new IOException(
                        "Could not authenticate connection, please try again.");

            // if authorized, create the session
            Session session = (Session) ssh.openSession();
            session.execCommand("mkdir test");

            // terminate the session
            session.close();

            // terminate the connection
            ssh.close();
        } catch (IOException e) {
            e.printStackTrace(System.err);
            System.out.println(e.getMessage());
            //System.exit(2);
        }
    }
}
