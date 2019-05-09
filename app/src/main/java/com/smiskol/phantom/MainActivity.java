package com.smiskol.phantom;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import ch.ethz.ssh2.Session;

public class MainActivity extends AppCompatActivity implements WelcomeFragment.OnFragmentInteractionListener, ControlsFragment.OnFragmentInteractionListener {
    SharedPreferences preferences;
    android.support.v7.widget.Toolbar toolbar;
    ViewPager viewPager;
    ViewPagerAdapter adapter;
    Typeface semibold;
    Typeface regular;
    TabLayout tabLayout;
    String eonIP;
    SSHClass sshClass = new SSHClass();
    Long goDown = Long.valueOf(0);
    Long goDuration = Long.valueOf(0);
    Boolean holdMessage = false;
    Boolean buttonHeld = false;
    Boolean runPhantomThread = true;
    Integer runningProcesses = 0;
    Integer maxProcesses = 1;
    Integer previousSteer = 0;
    Integer steeringTorque = 0;
    Double desiredSpeed = 10.0;
    Boolean trackingSteer = false;
    Boolean steerLetGo = false;
    Boolean phantomThreadRunning = false;
    Integer timeValue = 0;
    Session eonSession;
    Integer maxSteer;
    Boolean useMph;

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
        maxSteer = preferences.getInt("maxSteer", 1000);
        useMph = preferences.getBoolean("useMph", true);

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
            Integer threadSleep = 100;
            phantomThreadRunning = true;
            runPhantomThread = true;
            previousSteer = steeringTorque;
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
                    if (!previousSteer.equals(steeringTorque) && trackingSteer && !steerLetGo) {
                        previousSteer = steeringTorque;
                        publishProgress("move_with_wheel");
                    } else if (steerLetGo) {
                        previousSteer = steeringTorque;
                        steerLetGo = false;
                        publishProgress("move_with_wheel");
                    } else if (holdMessage) {
                        holdMessage = false;
                        publishProgress("move_message");
                    } else {
                        if (iterations > (500 / threadSleep)) {
                            iterations = 0;
                            publishProgress("move");
                        }
                    }
                } else if (!buttonHeld && !previousSteer.equals(steeringTorque) && trackingSteer) {
                    previousSteer = steeringTorque;
                    publishProgress("wheel");
                } else if (!buttonHeld && steerLetGo) {
                    previousSteer = steeringTorque;
                    steerLetGo = false;
                    publishProgress("wheel");
                } else if (showStopMessage) {
                    showStopMessage = false;
                    publishProgress("brake");
                } else {
                    if (iterations > (2000 / threadSleep)) {
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
            System.out.println(steeringTorque);
            if (method[0].equals("move") || method[0].equals("move_with_wheel") || method[0].equals("move_message")) {
                String[] params;
                if (useMph) {
                    params = new String[]{"true", String.valueOf(desiredSpeed * 0.44704), String.valueOf(steeringTorque), method[0]};
                } else {
                    params = new String[]{"true", String.valueOf(desiredSpeed / 3.6), String.valueOf(steeringTorque), method[0]};
                }
                new sendPhantomCommand().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
            } else if (method[0].equals("wheel")) {
                String[] params = new String[]{"true", "0", String.valueOf(steeringTorque), method[0]};
                new sendPhantomCommand().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
            } else if (method[0].equals("brake") || method[0].equals("brake_no_message")) {
                String[] params = new String[]{"true", "0", String.valueOf(steeringTorque), method[0]};
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
            AlertDialog successDialog = new AlertDialog.Builder(this).setTitle("Welcome!")
                    .setMessage("Phantom is an experimental app that can remotely control your car's acceleration and wheel torque via SSH.")
                    .setPositiveButton("Sick", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            infoDialog();
                        }
                    }).setCancelable(false)
                    .setIcon(R.mipmap.ic_launcher)
                    .show();

            int titleText = getResources().getIdentifier("alertTitle", "id", "android");
            ((TextView) successDialog.getWindow().findViewById(titleText)).setTypeface(semibold);
            TextView tmpMessage = successDialog.getWindow().findViewById(android.R.id.message);
            Button tmpButton = successDialog.getWindow().findViewById(android.R.id.button1);
            tmpMessage.setTypeface(regular);
            tmpButton.setTypeface(semibold);
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

    public void infoDialog() {
        AlertDialog successDialog = new AlertDialog.Builder(this).setTitle("Reboot recommended")
                .setMessage("After you first successfully connect with Phantom, a reboot of your EON is recommended. An ssh setting is appended to your sshd_config that greatly reduces the latency of sending Phantom commands.")
                .setPositiveButton("Got it", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        preferences.edit().putBoolean("welcome", false).apply();
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

    public Boolean openSession() {
        eonSession = sshClass.getSession(eonIP);
        if (eonSession == null) {
            System.out.println("failed to open connection");
            return false;
        }
        System.out.println("opened connection!");
        return true;
    }

    public String getTime() { //doesn't need to be time, just unique value for disconnect detection
        if (timeValue > 500) {
            timeValue = 0;
        } else {
            timeValue += 1;
        }
        return String.valueOf(timeValue);
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
            Boolean result = sshClass.sendPhantomCommand(eonSession, params[0], params[1], params[2], getTime());
            return new String[]{result.toString(), params[3]};
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

    public void makeSnackbar(String s) {
        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), s, Snackbar.LENGTH_SHORT);
        TextView tv = (snackbar.getView()).findViewById(android.support.design.R.id.snackbar_text);
        tv.setTypeface(regular);
        snackbar.show();
    }
}