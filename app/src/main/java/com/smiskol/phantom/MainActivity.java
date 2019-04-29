package com.smiskol.phantom;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.jcraft.jsch.Session;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements WelcomeFragment.OnFragmentInteractionListener, ControlsFragment.OnFragmentInteractionListener {
    SharedPreferences preferences;
    android.support.v7.widget.Toolbar toolbar;
    ViewPager viewPager;
    ViewPagerAdapter adapter;
    Typeface semibold;
    Typeface regular;
    TabLayout tabLayout;
    Session eonSession;
    String eonIP;
    SSHClass sshClass = new SSHClass();
    Long goDown = Long.valueOf(0);
    Long goDuration = Long.valueOf(0);
    Boolean holdMessage = false;
    Boolean buttonHeld = false;
    Boolean runPhantomThread = true;
    Integer runningProcesses = 0;
    Integer maxProcesses = 1;
    Double previousSteer = 0.0;
    Double steeringAngle = 0.0;
    Double desiredSpeed = 5.0;
    Boolean trackingSteer = false;
    Boolean steerLetGo = false;
    Boolean phantomThreadRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        toolbar = findViewById(R.id.toolBar);
        setSupportActionBar(toolbar);
        viewPager = findViewById(R.id.pager);
        adapter = new ViewPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(adapter);
        tabLayout = findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);
        semibold = ResourcesCompat.getFont(this, R.font.product_bold);
        regular = ResourcesCompat.getFont(this, R.font.product_regular);

        getSupportActionBar().hide();
        doWelcome();
        new GetCommits().execute();

    }

    public class GetCommits extends AsyncTask<Void, String, String> {
        protected String doInBackground(Void... params) {
            HttpURLConnection connection = null;
            BufferedReader reader = null;
            try {
                URL url = new URL("https://api.github.com/repos/ShaneSmiskol/phantom-app/commits");
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();
                InputStream stream = connection.getInputStream();
                reader = new BufferedReader(new InputStreamReader(stream));
                StringBuffer buffer = new StringBuffer();
                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line + "\n");
                }
                return buffer.toString();

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            if (result == null) {
                makeSnackbar("Unable to check for updates!");
            } else {
                try {
                    JSONArray commits = new JSONArray(result);
                    ArrayList<String> commitsSince = new ArrayList<>();
                    for (int commit = 0; commit < commits.length(); commit++) {
                        if (!commits.getJSONObject(commit).getString("sha").equals(getString(R.string.current_commit))) {
                            commitsSince.add(commits.getJSONObject(commit).getString("sha"));
                        } else {
                            if (commitsSince.size() > 1) {
                                commitsSince.remove(commitsSince.size() - 1);
                            } else {
                                commitsSince.remove(0);
                            }
                            break;
                        }
                    }
                    new CheckUpdate().execute(commitsSince);
                } catch (Exception e) {
                    makeSnackbar("Unable to check for updates!");
                    e.printStackTrace();
                }
            }
        }
    }

    public class CheckUpdate extends AsyncTask<ArrayList<String>, String, Boolean> {
        protected Boolean doInBackground(ArrayList<String>... commits) {
            ArrayList<String> commitsSince = commits[0];
            for (int commit = 0; commit < commitsSince.size(); commit++) {
                HttpURLConnection connection = null;
                BufferedReader reader = null;
                try {
                    URL url = new URL("https://api.github.com/repos/ShaneSmiskol/phantom-app/commits/" + commitsSince.get(commit));
                    connection = (HttpURLConnection) url.openConnection();
                    connection.connect();
                    InputStream stream = connection.getInputStream();
                    reader = new BufferedReader(new InputStreamReader(stream));
                    StringBuffer buffer = new StringBuffer();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        buffer.append(line + "\n");
                    }
                    JSONArray files = new JSONObject(buffer.toString()).getJSONArray("files");
                    for (int file = 0; file < files.length(); file++) {
                        if (files.getJSONObject(file).getString("filename").equals("phantom-app.apk")) {
                            return true;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                    try {
                        if (reader != null) {
                            reader.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result == null) {
                makeSnackbar("Unable to check for updates!");
            } else if (result) {
                outOfDate();
            } else {
                makeSnackbar("You're on the latest commit!");
            }
        }
    }

    @Override
    public void onFragmentInteraction(Uri uri) {
    }

    Boolean showStopMessage = false;

    public class PhantomThread extends AsyncTask<Void, String, Boolean> {
        @Override
        protected Boolean doInBackground(Void... v) {
            Integer iterations = 0;
            Integer threadSleep = 250;
            phantomThreadRunning = true;
            runPhantomThread = true;
            previousSteer = steeringAngle;
            System.out.println("started phantom thread");
            while (true) {
                System.out.println(runningProcesses);
                try {
                    Thread.sleep(threadSleep);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (runningProcesses > maxProcesses) {
                    while (runningProcesses > maxProcesses) {
                        System.out.println("waiting for excess processes to finish");
                        try {
                            Thread.sleep(threadSleep);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                if ((System.currentTimeMillis() - goDown) > 200 && buttonHeld) {
                    showStopMessage = true;
                    if (!previousSteer.equals(steeringAngle) && trackingSteer && !steerLetGo) {
                        previousSteer = steeringAngle;
                        publishProgress("move_with_wheel");
                    } else if (steerLetGo) {
                        previousSteer = steeringAngle;
                        steerLetGo = false;
                        publishProgress("move_with_wheel");
                    } else if (holdMessage) {
                        holdMessage = false;
                        publishProgress("move_message");
                    } else {
                        if (iterations > (3000 / threadSleep)) {
                            iterations = 0;
                            publishProgress("move");
                        }
                    }
                } else if (!buttonHeld && !previousSteer.equals(steeringAngle) && trackingSteer) {
                    previousSteer = steeringAngle;
                    publishProgress("wheel");
                } else if (!buttonHeld && steerLetGo) {
                    previousSteer = steeringAngle;
                    steerLetGo = false;
                    publishProgress("wheel");
                } else if (showStopMessage) {
                    showStopMessage = false;
                    publishProgress("brake");
                } else {
                    if (iterations > (3000 / threadSleep)) {
                        iterations = 0;
                        publishProgress("brake_no_message");
                    }
                }
                if (!runPhantomThread) {
                    return true;
                }
                iterations += 1;
            }
        }

        @Override
        protected void onProgressUpdate(String... method) {
            if (method[0].equals("move") || method[0].equals("move_with_wheel") || method[0].equals("move_message")) {
                String[] params = new String[]{"true", String.valueOf(desiredSpeed * 0.44704), String.valueOf(steeringAngle), String.valueOf(System.currentTimeMillis()), method[0]};
                new sendPhantomCommand().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
            } else if (method[0].equals("wheel")) {
                String[] params = new String[]{"true", "0", String.valueOf(steeringAngle), String.valueOf(System.currentTimeMillis()), method[0]};
                new sendPhantomCommand().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
            } else if (method[0].equals("brake") || method[0].equals("brake_no_message")) {
                String[] params = new String[]{"true", "0", String.valueOf(steeringAngle), String.valueOf(System.currentTimeMillis()), method[0]};
                new sendPhantomCommand().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            phantomThreadRunning = false;
            System.out.println("stopped phantom thread");
            if (runPhantomThread) {
                makeSnackbar("Lost connection to the EON!");
                doDisable();
            }
        }
    }

    public void doWelcome() {
        if (preferences.getBoolean("welcome", true)) {
            welcomeDialog();
        }
    }

    public void requestPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                1);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (preferences.getBoolean("welcome", true)) {
                    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        writeSupportingFiles();
                        preferences.edit().putBoolean("welcome", false).apply();
                        successDialog();
                    } else {
                        uhohDialog();
                    }
                }
            }
        }
    }

    public void outOfDate() {
        AlertDialog successDialog = new AlertDialog.Builder(this).setTitle("Out of date!")
                .setMessage("You're on an old version of Phantom. Updating is highly recommended.")
                .setPositiveButton("Update", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ShaneSmiskol/phantom-app/blob/master/README.md"));
                        startActivity(browserIntent);
                    }

                }).show();

        int titleText = getResources().getIdentifier("alertTitle", "id", "android");
        ((TextView) successDialog.getWindow().findViewById(titleText)).setTypeface(semibold);
        TextView tmpMessage = successDialog.getWindow().findViewById(android.R.id.message);
        Button tmpButton = successDialog.getWindow().findViewById(android.R.id.button1);
        tmpMessage.setTypeface(regular);
        tmpButton.setTypeface(semibold);
    }

    public void successDialog() {
        AlertDialog successDialog = new AlertDialog.Builder(this).setTitle("Granted!")
                .setMessage("You're all set now, Captain.")
                .setPositiveButton("Rad", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }

                }).show();
        int titleText = getResources().getIdentifier("alertTitle", "id", "android");
        ((TextView) successDialog.getWindow().findViewById(titleText)).setTypeface(semibold);
        TextView tmpMessage = successDialog.getWindow().findViewById(android.R.id.message);
        Button tmpButton = successDialog.getWindow().findViewById(android.R.id.button1);
        tmpMessage.setTypeface(regular);
        tmpButton.setTypeface(semibold);
    }

    public void warningDialog() {
        AlertDialog successDialog = new AlertDialog.Builder(this).setTitle("Warning")
                .setMessage("This is extremely experimental software you are about to try. You must accept all responsibility and be vigilant in controlling your car in the event of any malfunction. Proceed at your own risk.")
                .setPositiveButton("Groovy", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        preferences.edit().putBoolean("warning", true).apply();
                    }

                }).setCancelable(false)
                .show();

        int titleText = getResources().getIdentifier("alertTitle", "id", "android");
        ((TextView) successDialog.getWindow().findViewById(titleText)).setTypeface(semibold);
        TextView tmpMessage = successDialog.getWindow().findViewById(android.R.id.message);
        Button tmpButton = successDialog.getWindow().findViewById(android.R.id.button1);
        tmpMessage.setTypeface(regular);
        tmpButton.setTypeface(semibold);
    }

    public void uhohDialog() {
        AlertDialog successDialog = new AlertDialog.Builder(this).setTitle("Uh oh!")
                .setMessage("You've denied the storage permission. We need this to write the EON private key to a file so we can make connections over SSH.")
                .setPositiveButton("Retry", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }).setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        requestPermission();
                    }
                }).setCancelable(false)
                .show();

        int titleText = getResources().getIdentifier("alertTitle", "id", "android");
        ((TextView) successDialog.getWindow().findViewById(titleText)).setTypeface(semibold);
        TextView tmpMessage = successDialog.getWindow().findViewById(android.R.id.message);
        Button tmpButton = successDialog.getWindow().findViewById(android.R.id.button1);
        tmpMessage.setTypeface(regular);
        tmpButton.setTypeface(semibold);
    }

    public void welcomeDialog() {
        AlertDialog successDialog = new AlertDialog.Builder(this).setTitle("Welcome!")
                .setMessage("Phantom is an experimental app that can remotely control your car's acceleration and wheel angle via SSH. We will now request the data permission, required to access your EON.")
                .setPositiveButton("Sick", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }).setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        requestPermission();
                    }
                }).setCancelable(false)
                .show();

        int titleText = getResources().getIdentifier("alertTitle", "id", "android");
        ((TextView) successDialog.getWindow().findViewById(titleText)).setTypeface(semibold);
        TextView tmpMessage = successDialog.getWindow().findViewById(android.R.id.message);
        Button tmpButton = successDialog.getWindow().findViewById(android.R.id.button1);
        tmpMessage.setTypeface(regular);
        tmpButton.setTypeface(semibold);
    }

    public Boolean openSession(String eonIP) {
        try {
            eonSession = sshClass.getSession(MainActivity.this, eonIP);
            return true;
        } catch (Exception e) {
            System.out.println("HERE SADLY");
            e.printStackTrace();
            return false;
        }
    }

    public void doSuccessful() {
        adapter.setViewCount(2);
        viewPager.setCurrentItem(1);
        tabLayout.setVisibility(View.VISIBLE);
        new PhantomThread().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        if (!preferences.getBoolean("warning", false)) {
            warningDialog();
        }
    }

    public class sendPhantomCommand extends AsyncTask<String, Void, String[]> {

        @Override
        protected String[] doInBackground(String... params) {
            runningProcesses += 1;
            Boolean result = sshClass.sendPhantomCommand(eonSession, eonIP, params[0], params[1], params[2], params[3]);
            return new String[]{result.toString(), params[4]};
        }

        @Override
        protected void onPostExecute(String... result) {
            runningProcesses -= 1;
            if (result[0].equals("true")) {
                if (result[1].equals("enable")) {
                    doSuccessful();
                    makeSnackbar("Enabled Phantom!");
                } else if (result[1].equals("disable")) {
                    doDisable();
                    makeSnackbar("Disabled Phantom!");
                } else if (result[1].equals("brake")) {
                    makeSnackbar("Stopping car!");
                    System.out.println("stopping car");
                } else if (result[1].equals("brake_no_message")) {
                    System.out.println("brake command");
                } else if (result[1].equals("move_message")) {
                    System.out.println("moving update");
                    makeSnackbar("Moving car...");
                } else if (result[1].equals("wheel")) {
                    System.out.println("wheel update");
                } else if (result[1].equals("move_with_wheel")) {
                    System.out.println("move+wheel update");
                } else if (result[1].equals("move")) {
                    System.out.println("move update");
                }
            } else {
                if (result[1].equals("disable")) {
                    makeSnackbar("Error disabling Phantom mode!");
                } else {
                    doDisable();
                    makeSnackbar("Couldn't connect to EON! Perhaps wrong IP?");
                }
            }
        }
    }


    public void doDisable() {
        viewPager.setCurrentItem(0);
        adapter.setViewCount(1);
        tabLayout.setVisibility(View.GONE);
    }

    public void writeSupportingFiles() {
        try {
            File file = new File(getFilesDir(), "eon_id.ppk");
            if (!file.exists() || file.length() == 0) {
                FileOutputStream fOut = new FileOutputStream(file);
                OutputStreamWriter osw = new OutputStreamWriter(fOut);

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

                osw.write(privateKey);

                osw.close();
                System.out.println("File written");
            } else {
                System.out.println("File already written");
            }

        } catch (IOException ioe) {
            ioe.printStackTrace();
            makeSnackbar("Error writing support files.");
        }
    }

    public void makeSnackbar(String s) {
        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), s, Snackbar.LENGTH_SHORT);
        TextView tv = (snackbar.getView()).findViewById(android.support.design.R.id.snackbar_text);
        tv.setTypeface(regular);
        snackbar.show();
    }

}