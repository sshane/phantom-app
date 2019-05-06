package com.smiskol.phantom;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.res.ResourcesCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import org.w3c.dom.Text;

public class WelcomeFragment extends Fragment {
    TextView listeningTextView;
    Switch connectSwitch;
    EditText ipEditText;
    TextInputLayout ipEditTextLayout;
    Button settingsButton;
    Typeface semibold;
    Typeface regular;
    RelativeLayout settingsDialogView;
    MainActivity context;

    private OnFragmentInteractionListener mListener;

    public WelcomeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_welcome, container, false);
        listeningTextView = view.findViewById(R.id.connectedTextNew);
        connectSwitch = view.findViewById(R.id.connectSwitchNew);
        ipEditTextLayout = view.findViewById(R.id.ipEditTextLayoutNew);
        ipEditText = view.findViewById(R.id.ipEditTextNew);
        settingsButton = view.findViewById(R.id.settingsButton);
        context = ((MainActivity) getActivity());
        startListeners();
        ipEditText.setText(context.preferences.getString("eonIP", ""));
        semibold = ResourcesCompat.getFont(context, R.font.product_bold);
        regular = ResourcesCompat.getFont(context, R.font.product_regular);
        settingsDialogView = view.findViewById(R.id.settingsDialogView);
        return view;
    }

    public void startListeners() {
        connectSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!buttonView.isPressed()) {
                    return;
                }
                if (isChecked) {
                    if (!ipEditText.getText().toString().equals("") && ipEditText.getText().toString().length() >= 7) {
                        ipEditText.setEnabled(false);
                        connectSwitch.setEnabled(false);
                        listeningTextView.setText("Testing connection...");
                        new openSession().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    } else {
                        connectSwitch.setChecked(false);
                        makeSnackbar("Please enter an IP!");
                        Animation mShakeAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.shake);
                        ipEditTextLayout.startAnimation(mShakeAnimation);
                    }
                } else {
                    connectSwitch.setEnabled(false);
                    connectSwitch.setChecked(true);
                    context.runPhantomThread = false;
                    String[] params = new String[]{"false", "0", "0", "disable"};
                    new sendPhantomCommand().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params); //disable phantom mode on EON
                    listeningTextView.setText("Disabling...");
                }
            }
        });
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSettingsDialog();
            }
        });
    }

    public void openSettingsDialog() {
        final AlertDialog settingsDialog = new AlertDialog.Builder(context).setTitle("Settings")
                .setMessage("")
                .setPositiveButton("Sick", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //infoDialog();
                    }
                }).show();
        if (settingsDialogView.getParent() != null) {
            ((ViewGroup) settingsDialogView.getParent()).removeView(settingsDialogView);
        }
        settingsDialogView.setVisibility(View.VISIBLE);

        settingsDialog.setContentView(settingsDialogView);
        Button settingsSaveButton = settingsDialog.findViewById(R.id.settingsSaveButton);
        SeekBar maxAngleSeekBar = settingsDialog.findViewById(R.id.maxAngleSeekBar);
        final TextView maxSteerText = settingsDialog.findViewById(R.id.maxSteerTextDeg);
        Switch unitSwitch = settingsDialog.findViewById(R.id.unitSwitch);
        context.maxSteer = context.preferences.getInt("maxSteer", 100);
        maxAngleSeekBar.setProgress(interp(context.maxSteer, 100, 360, 0, 100));
        maxSteerText.setText(context.maxSteer + "°");
        maxAngleSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                context.maxSteer = interp(progress, 0, 100, 100, 360);
                maxSteerText.setText(interp(progress, 0, 100, 100, 360) + "°");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        context.useMph=context.preferences.getBoolean("useMph", true);
        unitSwitch.setChecked(context.preferences.getBoolean("useMph", true));
        unitSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                context.useMph=isChecked;
            }
        });

        settingsSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                makeSnackbar("Saved settings!");
                context.preferences.edit().putInt("maxSteer", context.maxSteer).apply();
                context.preferences.edit().putBoolean("useMph", context.useMph).apply();
                settingsDialog.dismiss();
            }
        });
    }

    public Integer interp(int value, int from1, int to1, int from2, int to2) {
        return (int) Math.round(((Double.valueOf(value)) - (Double.valueOf(from1))) / ((Double.valueOf(to1)) - (Double.valueOf(from1))) * ((Double.valueOf(to2)) - (Double.valueOf(from2))) + (Double.valueOf(from2)));
    }

    public class openSession extends AsyncTask<String, Void, Boolean> {

        @Override
        protected Boolean doInBackground(String... params) {
            return context.openSession(ipEditText.getText().toString());
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                String[] params = new String[]{"true", "0", "0", "enable"};
                new sendPhantomCommand().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params); //enable phantom mode on EON
            } else {
                doDisable();
                makeSnackbar("Couldn't connect to EON! Perhaps wrong IP?");

            }
        }
    }

    public class sendPhantomCommand extends AsyncTask<String, Void, String[]> {
        @Override
        protected String[] doInBackground(String... params) {
            if (params[3].equals("disable")) {
                while (context.phantomThreadRunning) {
                    try {
                        Thread.sleep(50);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                try {
                    Thread.sleep(500);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            context.runningProcesses += 1;
            Boolean result = context.sshClass.sendPhantomCommand(context.eonSession, params[0], params[1], params[2], context.getTime());
            return new String[]{result.toString(), params[3]};
        }

        @Override
        protected void onPostExecute(String... result) {
            context.runningProcesses -= 1;
            if (result[0].equals("true")) {
                if (result[1].equals("enable")) {
                    doSuccessful();
                    makeSnackbar("Enabled Phantom!");
                } else if (result[1].equals("disable")) {
                    doDisable();
                    System.out.println("disabled phantom mode");
                    makeSnackbar("Disabled Phantom!");
                } else if (result[1].equals("brake")) {
                    makeSnackbar("Stopping car!");
                    System.out.println("stopping car");
                } else if (result[1].equals("move")) {
                    System.out.println("moving update");
                    makeSnackbar("Moving car...");
                } else if (result[1].equals("wheel")) {
                    System.out.println("wheel update");
                } else if (result[1].equals("move_with_wheel")) {
                    System.out.println("move+wheel update");
                }
            } else {
                if (result[1].equals("disable")) {
                    connectSwitch.setEnabled(true);
                    makeSnackbar("Error disabling Phantom mode!");
                } else {
                    doDisable();
                    makeSnackbar("Couldn't connect to EON! Perhaps wrong IP?");
                }
            }
        }
    }

    public void doDisable() {
        context.doDisable();
        connectSwitch.setChecked(false);
        connectSwitch.setEnabled(true);
        listeningTextView.setText("Not Connected");
        ipEditText.setEnabled(true);
    }

    public void doSuccessful() {
        context.doSuccessful();
        connectSwitch.setChecked(true);
        connectSwitch.setEnabled(true);
        context.eonIP = ipEditText.getText().toString();
        context.preferences.edit().putString("eonIP", ipEditText.getText().toString()).apply();
        listeningTextView.setText("Connected!");
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    public void makeSnackbar(String s) {
        Snackbar snackbar = Snackbar.make(getActivity().findViewById(android.R.id.content), s, Snackbar.LENGTH_SHORT);
        TextView tv = (snackbar.getView()).findViewById(android.support.design.R.id.snackbar_text);
        Typeface font = ResourcesCompat.getFont(getActivity(), R.font.product_regular);
        tv.setTypeface(font);
        snackbar.show();
    }
}
