package com.smiskol.phantom;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

public class WelcomeFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    //private static final String ARG_PARAM1 = "param1";
    //private static final String ARG_PARAM2 = "param2";
    CustomSeekBar steerSeekBar;
    TextView steerTextView;
    TextView speedTextView;
    SeekBar accelSeekBar;
    TextView listeningTextView;
    Switch connectSwitch;
    EditText ipEditText;
    TextView titleTextView;
    TextInputLayout ipEditTextLayout;
    CardView steerCard;
    CardView cardViewMain;
    CardView accelCard;
    LinearLayout welcomeLayoutTitle;
    LinearLayout welcomeLayout;
    LinearLayout connectLayout;
    LinearLayout steerLayout;
    LinearLayout accelLayout;
    Button goButton;
    LinearLayout holdButton;
    ImageButton speedPlusButton;
    ImageButton speedSubButton;


    private OnFragmentInteractionListener mListener;

    public WelcomeFragment() {
        // Required empty public constructor
    }
    
    public static WelcomeFragment newInstance(String param1, String param2) {
        WelcomeFragment fragment = new WelcomeFragment();
        //Bundle args = new Bundle();
        //args.putString(ARG_PARAM1, param1);
        //args.putString(ARG_PARAM2, param2);
        //fragment.setArguments(args);
        return fragment;
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
        startListeners();
        ipEditText.setText(((MainActivity) getActivity()).preferences.getString("eonIP", ""));
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
                        String[] params = new String[]{"true", "0.0", "0", "0", "enable"};
                        new sendPhantomCommand().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params); //enable phantom mode on EON
                    } else {
                        connectSwitch.setChecked(false);
                        makeSnackbar("Please enter an IP!");
                        Animation mShakeAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.shake);
                        ipEditTextLayout.startAnimation(mShakeAnimation);
                    }


                } else {
                    connectSwitch.setEnabled(false);
                    connectSwitch.setChecked(true);
                    String[] params = new String[]{"false", "0.0", "0", "0", "disable"};
                    new sendPhantomCommand().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params); //disable phantom mode on EON
                    listeningTextView.setText("Disabling...");
                }
            }
        });
    }

    public class sendPhantomCommand extends AsyncTask<String, Void, String[]> {

        @Override
        protected String[] doInBackground(String... params) {
            ((MainActivity) getActivity()).runningProcesses += 1;
            Boolean result = new SSHClass().sendPhantomCommand(getActivity(), ipEditText.getText().toString(), params[0], params[1], params[2], params[3]);
            return new String[]{result.toString(), params[4]};
        }

        @Override
        protected void onPostExecute(String... result) {
            ((MainActivity) getActivity()).runningProcesses -= 1;
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
        ((MainActivity) getActivity()).doDisable();
        connectSwitch.setChecked(false);
        connectSwitch.setEnabled(true);
        listeningTextView.setText("Not Connected");
        ipEditText.setEnabled(true);
    }

    public void doSuccessful() {
        ((MainActivity) getActivity()).doSuccessful();
        //new PhantomThread().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        connectSwitch.setChecked(true);
        connectSwitch.setEnabled(true);
        ((MainActivity) getActivity()).preferences.edit().putString("eonIP", ipEditText.getText().toString()).apply();
        listeningTextView.setText("Connected!");
                /*if (!preferences.getBoolean("warning", false)) {
                    warningDialog();
                }*/
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
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
        TextView tv = (TextView) (snackbar.getView()).findViewById(android.support.design.R.id.snackbar_text);
        Typeface font = ResourcesCompat.getFont(getActivity(), R.font.product_regular);
        tv.setTypeface(font);
        snackbar.show();
    }
}
