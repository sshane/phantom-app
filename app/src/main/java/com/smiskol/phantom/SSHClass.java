package com.smiskol.phantom;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;

public class SSHClass {
    private static final String TAG = "SSHClass";
    String privateKey = "-----BEGIN RSA PRIVATE KEY-----\n" +
            "MIIEogIBAAKCAQEAvol16t9E6vieTSmrdylhws3JsGeeZxoeloIAKhAmuQmrAZTP\n" +
            "VXkTqVbt23gPuYdDIm0YGw+AzLVVwbeoBL2fJ3dOBO3iwPS02chQ2e0pEjlY+KFz\n" +
            "kLE9BpyZiqwEluSrJU1qlc036NlwrWftNOIpC8ZshXgTvDTnBK1taWvIBXUA06B/\n" +
            "RawO5IMrInP11REkzqHu15c0aHv3mWnBEPo7Z5hXdtQOGhAA5JNNAIY69LimiYi1\n" +
            "AD2rcbNonCF1qYGLX6qrWihdt8EretTk7unAMF2zlq95viFEkVDtCEcxCEEt89Y3\n" +
            "3dbL4M0oEksGdS4Y+AKCsSBACHPKiazuLspgiQIDAQABAoIBAQCEhXr8RxnaC92e\n" +
            "cZMOqDuUkCjthsRHlYUczYJrvxwPqsfDq8qg+jtQlmONN+5H7eolsZcIizncJ2tj\n" +
            "9ubnlTNy8anUB9ikuA5pQsfpKuhcAoL9Ot30DzIQvS6Vopr2kEjxAu1VD40JaOLT\n" +
            "2OrE02AVDodANYoUZv8e47irkAlosQqvAvw1ZwdV+Jho/lt5yXOU8FSbYCW24ga6\n" +
            "uj1q4bwf96ppMR0S+3VNkgW9ojURdSy2N9HScf3A+91AyjR65a7I5N1CXNvTKePz\n" +
            "JWnSr1JEajcJWMUrgLSVdJ2d/ohZC7N2nUkx3SaQpUHq+OUedaxQ5VbA89mQaW/4\n" +
            "UTUaBg7hAoGBAOgNRIsS6u0GDod3G14cod1uJKVbwPxT3yh9TjMtzjTg/2PTmvjP\n" +
            "8LYVtcEqES9p/rriFuTgIUyLyBIr4+mwGbE097cK7zq72Lva8fWpZ+KfAYcr3Y3l\n" +
            "uJEu0/BT+aJei6DrdrEz909SzriTzrkLzo5SjyiDId3N0RTVk5xszD2tAoGBANIz\n" +
            "Yjy8T9wNp619JHigyPlR7rzmPPIHYdFuQbizC6LziA5PWkBSGwWzLltTk4xyr/TS\n" +
            "vi68PmGwhajhn9XVP1DeYEshPJV/0BbFBlKlGcee+JyWZziHMtzjTp0C3LxwEE6C\n" +
            "xQBlHez1oD9wrR5LfYRL9pKFMC+L6IpEz9bvRpHNAoGBANmqaFsT2a2Pet1yygcT\n" +
            "UHnGMTWyxWlquu7d6xZypvRPAQCAouM1GhOSdbTFYu1YvYpLPTJfUpzcmUUCSn0P\n" +
            "pGnmx125MgGj5n7/tuq6hym6ANLsQJwzmVcF1+OcwZKeoNbHR8ScfCS6BhJ5AvXs\n" +
            "r0otAv/7US8fOjoSxK18GHDZAn9YrVTESq1mKFyU1DaOrUYb6HTPPFJ5yKN7twgC\n" +
            "44YFOLgtUUzB1eGQhgcIgDm/BqM0pbOWA9RNYisBFC5aB5yugSIej+b/Kuyern/8\n" +
            "XaqCjI5VgR4Kuv66MSr5EjwNQzmd5Y02nXIChZ0VJnPiU/af2WwsZAPwCxYPPvhv\n" +
            "tIIRAoGAPLxtzP7rcHi76uESO5e1O2/otgWo3ytjpszYv8boH3i42OpNrX0Bkbr+\n" +
            "qaU43obY4trr4A1pIIyVID32aYq9yEbFTFIhYJaFhhxEzstEL3OQMLakyRS0w9Vs\n" +
            "2trgYpUlSBLIOmPNxonJIfnozphLGOnKNe0RWgGR8BnwhRYzu+k=\n" +
            "-----END RSA PRIVATE KEY-----\n";

    Connection connection = null;
    Session session = null;
    OutputStream os = null;
    Integer connectTimeout = 5000;
    InputStream stdout;
    BufferedReader br;


    public int openConnection(String eonIP) {
        /* Return codes:
        0: Successful
        1: Not authenticated
        2: Connection timeout
        3: Invalid IP Address
        4: Generic exception
        */
        String username = "root";
        try {
            connection = new Connection(eonIP, 8022);
            connection.connect(null, connectTimeout, connectTimeout);
            Boolean isAuthenticated = connection.authenticateWithPublicKey(username, privateKey.toCharArray(), "");
            if (!isAuthenticated) {
                Log.v(TAG, "Not authenticated!");
                session = null;
                return 1;
            } else {
                session = connection.openSession();
                session.requestDumbPTY();
                session.startShell();

                stdout = new ch.ethz.ssh2.StreamGobbler(session.getStdout());
                br = new BufferedReader(new InputStreamReader(stdout));
                os = session.getStdin();

                os.write("cd /data/openpilot\n".getBytes());
                os.write("python\n".getBytes());
                os.write("from selfdrive.phantom_receiver import PhantomReceiver\n".getBytes());
                os.write("PR=PhantomReceiver()\n".getBytes());
                os.write("PR.enable_phantom()\n".getBytes());

                //waitForEON("ENABLED");  //probably not required here
                return 0;
            }
        } catch (java.net.SocketTimeoutException e) {
            Log.e(TAG, "Connection timeout!");
            e.printStackTrace();
            session = null;
            return 2;
        } catch (IOException e) {
            Log.e(TAG, "Invalid IP address!");
            e.printStackTrace();
            session = null;
            return 3;
        } catch (Exception e) {
            Log.e(TAG, "Exception in getting session!");
            e.printStackTrace();
            session = null;
            return 4;
        }
    }

    public Boolean sendPhantomCommand(String enabled, String desiredSpeed, String steeringAngle, String time) {
        try {
            if (enabled.equals("true") || enabled.equals("True")) {
                enabled = "True";
            } else {
                enabled = "False";
            }
            if (enabled.equals("True")) {
                String command = "PR.receive_data(" + desiredSpeed + "," + steeringAngle + "," + time + ")\n";
                System.out.println(command);
                os.write(command.getBytes());
            } else {  //close all sessions and socks
                try {
                    os.write("PR.disable_phantom()\n".getBytes());

                    System.out.println("Waiting for EON to report back as disabled!");
                    waitForEON("DISABLED");
                    os.write("exit()\n".getBytes());
                    os.write("exit\n".getBytes());

                } catch (Exception e) {
                    System.out.println("Exception in closing session!");
                    e.printStackTrace();
                } finally {
                    os.close();
                    session.close();
                    connection.close();
                }
            }
            return true;
        } catch (Exception e) {
            System.out.println("Exception in sending command!");
            e.printStackTrace();
        }
        return false;
    }

    public void waitForEON(String function) {
        try {
            String line = "";
            while (br.ready()) { //wait until eon reports back phantom status changed
                line = br.readLine();
                if (line.contains(function)) {
                    System.out.println("Line contains " + function);
                    //System.out.println(line);
                    break;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error getting EON output!");
        }
    }

}
