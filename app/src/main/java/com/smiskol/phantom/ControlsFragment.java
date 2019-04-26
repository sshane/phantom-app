package com.smiskol.phantom;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.res.ResourcesCompat;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class ControlsFragment extends Fragment {
    CustomSeekBar steerSeekBar;
    TextView steerTextView;
    LinearLayout holdButton;
    ImageButton speedPlusButton;
    ImageButton speedSubButton;
    TextView speedTextView;


    private OnFragmentInteractionListener mListener;

    public ControlsFragment() {
        // Required empty public constructor
    }


    public static ControlsFragment newInstance(String param1, String param2) {
        ControlsFragment fragment = new ControlsFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_controls, container, false);
        steerSeekBar = view.findViewById(R.id.steerSeekBarNew);
        steerTextView = view.findViewById(R.id.steerTextViewNew);
        speedPlusButton = view.findViewById(R.id.speedPlusButton);
        speedSubButton = view.findViewById(R.id.speedSubButton);
        holdButton = view.findViewById(R.id.holdButton);
        speedTextView = view.findViewById(R.id.speedTextView);
        setUpListeners();
        setUpHoldButton();
        return view;
    }

    @SuppressLint("ClickableViewAccessibility")
    public void setUpHoldButton() {
        holdButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    System.out.println("move button down");
                    TransitionDrawable transition = (TransitionDrawable) holdButton.getBackground();
                    transition.startTransition(175);
                    ((MainActivity) getActivity()).goDown = System.currentTimeMillis();
                    ((MainActivity) getActivity()).holdMessage = true;
                    ((MainActivity) getActivity()).buttonHeld = true;
                } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                    System.out.println("move button up");
                    TransitionDrawable transition = (TransitionDrawable) holdButton.getBackground();
                    transition.reverseTransition(175);
                    ((MainActivity) getActivity()).holdMessage = false;
                    ((MainActivity) getActivity()).buttonHeld = false;
                    ((MainActivity) getActivity()).goDuration = System.currentTimeMillis() - ((MainActivity) getActivity()).goDown;
                    if (((MainActivity) getActivity()).goDuration < 200) {
                        if (!(event.getAction() == MotionEvent.ACTION_CANCEL)) {
                            makeSnackbar("You must hold button down for acceleration!");
                        }
                    } else {
                        String[] params = new String[]{"true", "0.0", String.valueOf(((MainActivity) getActivity()).steeringAngle), "0", "brake"};
                        new sendPhantomCommand().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
                    }
                    System.out.println("Button held for " + ((MainActivity) getActivity()).goDuration + " ms");
                }
                return false;
            }
        });
    }

    public class sendPhantomCommand extends AsyncTask<String, Void, String[]> {

        @Override
        protected String[] doInBackground(String... params) {
            ((MainActivity) getActivity()).runningProcesses += 1;

            Boolean result = ((MainActivity) getActivity()).sshClass.sendPhantomCommand(((MainActivity) getActivity()).eonSession, ((MainActivity) getActivity()).eonIP, params[0], params[1], params[2], params[3]);
            return new String[]{result.toString(), params[4]};
        }

        @Override
        protected void onPostExecute(String... result) {
            ((MainActivity) getActivity()).runningProcesses -= 1;
            if (result[0].equals("true")) {
                if (result[1].equals("brake")) {
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
                makeSnackbar("Couldn't connect to EON! Perhaps wrong IP?");
            }
        }
    }


    public void setUpListeners() {
        steerSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //steerTextView.setText(-(progress - 100) + "°");
                steerTextView.setText("TORQUE: " + Math.round(((progress - 100) / 100.0) * 1500.0));
                ((MainActivity) getActivity()).steeringAngle = -Math.round(((progress - 100) / 100.0) * 1500.0);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                ((MainActivity) getActivity()).trackingSteer = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                ((MainActivity) getActivity()).trackingSteer = false;
                if (seekBar.getProgress() > 100) {
                    final Integer endProgress = (steerSeekBar.getProgress() - 100) / 2;
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            for (int l = 0; l <= endProgress; l++) {
                                steerSeekBar.setProgress(steerSeekBar.getProgress() - 2);
                                try {
                                    Thread.sleep(2);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            steerSeekBar.setProgress(100);
                            ((MainActivity) getActivity()).steerLetGo = true;
                        }
                    }).start();
                } else if (seekBar.getProgress() < 100) {
                    final Integer endProgress = (100 - steerSeekBar.getProgress()) / 2;
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            for (int l = 0; l <= endProgress; l++) {
                                steerSeekBar.setProgress(steerSeekBar.getProgress() + 2);
                                try {
                                    Thread.sleep(2);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            steerSeekBar.setProgress(100);
                            ((MainActivity) getActivity()).steerLetGo = true;
                        }
                    }).start();
                }
            }
        });
        speedPlusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) getActivity()).desiredSpeed = Math.min(((MainActivity) getActivity()).desiredSpeed + 0.5, 10);
                speedTextView.setText(String.valueOf(((MainActivity) getActivity()).desiredSpeed) + " mph");
            }
        });
        speedSubButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) getActivity()).desiredSpeed = Math.max(((MainActivity) getActivity()).desiredSpeed - 0.5, 2);
                speedTextView.setText(String.valueOf(((MainActivity) getActivity()).desiredSpeed) + " mph");
            }
        });
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

    public void makeSnackbar(String s) {
        Snackbar snackbar = Snackbar.make(getActivity().findViewById(android.R.id.content), s, Snackbar.LENGTH_SHORT);
        TextView tv = (TextView) (snackbar.getView()).findViewById(android.support.design.R.id.snackbar_text);
        Typeface font = ResourcesCompat.getFont(getActivity(), R.font.product_regular);
        tv.setTypeface(font);
        snackbar.show();
    }


    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
