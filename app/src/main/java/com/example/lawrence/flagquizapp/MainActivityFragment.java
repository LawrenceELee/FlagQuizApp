package com.example.lawrence.flagquizapp;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class MainActivityFragment extends Fragment {

    private static final String TAG = "FlagQuizActivity";
    private static final int FLAGS_IN_QUIZ = 10;

    // View widgets
    private LinearLayout quizLinearLayout; // layout that contains the quiz
    private TextView questionNumberTextView; // shows current question #
    private ImageView flagImageView; // displays a flag
    private LinearLayout[] guessLinearLayouts; // rows of answer Buttons
    private TextView answerTextView; // displays correct answer

    private List<String> fileNameList; // flag file names
    private List<String> quizCountriesList; // countries in current quiz
    private Set<String> regionsSet; // world regions in current quiz
    private String correctAnswer; // correct country for the current flag

    // used to display stats at end of quiz
    private int totalGuesses;
    private int correctAnswers;
    private int guessRows;

    private SecureRandom random; // used to randomize the quiz
    private Handler handler; // used to delay loading next flag
    private Animation shakeAnimation; // animation for incorrect guess

    // inflates GUI and initialize most of the instance variables.
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.fragment_main, container, false);
        // inflate takes layout resource id,
        // ViewGroup (layout object) in which the Fragment will be displayed,
        // a boolean indicating whether or not the inflated GUI needs to be attached to the ViewGroup
        // (in a fragment's onCreateView() it should always be "false" since system auto attaches
        // fragment to appropriate host Activity's ViewGroup.
        // Store view to local variable that will be return by onCreateView()

        fileNameList = new ArrayList<>();
        quizCountriesList = new ArrayList<>();
        random = new SecureRandom();
        handler = new Handler();

        // load shake animation, repeat animation 3 times.
        shakeAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.incorrect_shake);
        shakeAnimation.setRepeatCount(3);

        // get refs to GUI widgets
        quizLinearLayout = (LinearLayout) view.findViewById(R.id.quizLinearLayout);
        questionNumberTextView = (TextView) view.findViewById(R.id.questionNumberTextView);
        flagImageView = (ImageView) view.findViewById(R.id.flagImageView);
        guessLinearLayouts = new LinearLayout[4];
        guessLinearLayouts[0] = (LinearLayout) view.findViewById(R.id.row1LinearLayout);
        guessLinearLayouts[1] = (LinearLayout) view.findViewById(R.id.row2LinearLayout);
        guessLinearLayouts[2] = (LinearLayout) view.findViewById(R.id.row3LinearLayout);
        guessLinearLayouts[3] = (LinearLayout) view.findViewById(R.id.row4LinearLayout);
        answerTextView = (TextView) view.findViewById(R.id.answerTextView);

        // configure listeners for buttons
        for( LinearLayout row : guessLinearLayouts ){
            for( int col=0; col < row.getChildCount(); ++col ){
                Button button = (Button) row.getChildAt(col);
                button.setOnClickListener(guessButtonListener);
            }
        }

        // set questionNumberTextView using string resource
        // on first run it will be "Question 1 of 10"
        questionNumberTextView.setText(getString(R.string.question, 1, FLAGS_IN_QUIZ));

        // return the fragment's view for display
        return view;
    }

    // set up & start next quiz
    public void resetQuiz() {
        // use AssetManager to get img file names
        AssetManager assets = getActivity().getAssets();
        fileNameList.clear();       // clear list after we load them.

        try{
            // load all flag image files from selected regions
            for(String region : regionsSet){
                String[] paths = assets.list(region);

                // remove the ".png" file extension
                for(String path : paths){
                    fileNameList.add(path.replace(".png", ""));
                }
            }
        } catch (IOException ioe) {
            Log.e(TAG, "Error loading image file names: ", ioe);
        }

        // clear previous games
        correctAnswers = 0;
        totalGuesses = 0;
        quizCountriesList.clear();

        int flagCounter = 1;
        int numOfFlags = fileNameList.size();

        // add 10 random file names to quizCounter
        while( flagCounter <= FLAGS_IN_QUIZ ){
            int randIdx = random.nextInt(numOfFlags);

            String filename = fileNameList.get(randIdx);

            // if region enabled and hasn't already been chosen
            if( !quizCountriesList.contains(filename) ){
                quizCountriesList.add(filename);
                ++flagCounter;
            }
        }

        // start quiz by loading first flag
        loadNextFlag();
    }

    // method to load next flag after correct guess
    private void loadNextFlag() {
        String nextImage = quizCountriesList.remove(0);
        correctAnswer = nextImage;      // update correct answer for this flag
        answerTextView.setText("");     // clear "correct" or "incorrect"

        // display current question number
        questionNumberTextView.setText(getString(R.string.question, correctAnswers+1, FLAGS_IN_QUIZ));

        // extract region from image's name
        String region = nextImage.substring(0, nextImage.indexOf('-'));

        // load next image from AssetManager
        AssetManager assets = getActivity().getAssets();

        InputStream stream = null;  // local variable don't get auto initialized to null.
        try{
            stream = assets.open(region + "/" + nextImage + ".png");
            Drawable flag = Drawable.createFromStream(stream, nextImage);
            flagImageView.setImageDrawable(flag);
            animate(false);     // animate image onto screen
        } catch(IOException ioe) {
            Log.e(TAG, "Error loading: " + nextImage, ioe);
        } finally {
            try{
                if( stream != null ) stream.close();
            } catch(Exception e){
                Log.e(TAG, "Error closing stream: ", e);
            }
        }

        // randomize filenames
        Collections.shuffle(fileNameList);

        // put correct answer at end of fileNameList
        int correct = fileNameList.indexOf(correctAnswer);
        fileNameList.add(fileNameList.remove(correct));

        // add 2, 4, etc num of guess buttons
        for( int row=0; row < guessRows; ++row ){
            for( int col=0; col < guessLinearLayouts[row].getChildCount(); ++col ){
                // get refs to button so that it can be configured
                Button newGuessButton = (Button) guessLinearLayouts[row].getChildAt(col);
                newGuessButton.setEnabled(true);

                // set text on button to country name
                String filename = fileNameList.get((row * 2) + col);
                newGuessButton.setText(getCountryName(filename));
            }
        }

        // randomly replace one button with the correct answer
        int row = random.nextInt(guessRows);
        int col = random.nextInt(2);
        LinearLayout randomRow = guessLinearLayouts[row];
        String countryName = getCountryName(correctAnswer);
        ((Button) randomRow.getChildAt(col)).setText(countryName);
    }

    // method to animate entire quizLinearLayout (on or off screen)
    public void animate(boolean animateOut){
        // no animation for first flag
        if( correctAnswers == 0 )       return;

        // animations only for SDK 21 or higher
        if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) return;

        // calc mid point for x & y axis
        int centerX = (quizLinearLayout.getLeft() + quizLinearLayout.getRight()) / 2;
        int centerY = (quizLinearLayout.getTop() + quizLinearLayout.getBottom()) / 2;

        // calc animation radius
        int radius = Math.max(quizLinearLayout.getWidth(), quizLinearLayout.getHeight());

        Animator animator;

        // if quizLinearLayout should animate "out" rather than "in"
        if( animateOut ){
            // create circular reveal animation
            animator = ViewAnimationUtils.createCircularReveal(
                    quizLinearLayout, centerX, centerY, radius, 0
            );

            animator.addListener(new AnimatorListenerAdapter() {
                // callback when animation finishes will load next flag
                @Override
                public void onAnimationEnd(Animator animation) {
                    loadNextFlag();
                }
            });

        } else {
            // quizLinearLayout should animate "in"
            animator = ViewAnimationUtils.createCircularReveal(
                    quizLinearLayout, centerX, centerY, 0, radius
            );
        }

        animator.setDuration(500);      // duration is 500 millisecs
        animator.start();               // start animation
    }

    private OnClickListener guessButtonListener = new OnClickListener() {

        @Override
        public void onClick(View view) {
            Button guessButton = (Button) view;
            String guess = guessButton.getText().toString();
            String answer = getCountryName(correctAnswer);
            ++totalGuesses;

            // correct guess
            if( guess.equals(answer) ){
                ++correctAnswers;

                // display correct answer in green text
                answerTextView.setText(answer + "!");
                answerTextView.setTextColor(Color.GREEN);

                disableButtons();

                // if user has gotten all correct
                if( correctAnswers == FLAGS_IN_QUIZ ){
                    // create a dialog alert with quiz stats
                    DialogFragment quizResults = new DialogFragment(){
                        @NonNull
                        @Override
                        public Dialog onCreateDialog(Bundle savedInstanceState) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

                            builder.setMessage(
                                    getString(R.string.results,
                                              totalGuesses,
                                              (1000/(double) totalGuesses
                                    )
                            ));

                            builder.setPositiveButton(
                                    R.string.reset_quiz,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i){
                                            resetQuiz();
                                        }
                                    }
                            );

                            return builder.create();
                        }
                    };

                    // use FragmentManger to display the DialogFragment
                    quizResults.setCancelable(false);
                    quizResults.show(getFragmentManager(), "quiz results");

                } else { // answer is correct but quiz is not over
                    // load next flag after a 2-second delay
                    handler.postDelayed(
                            new Runnable() {
                                @Override
                                public void run() {
                                    animate(true); // animate flag off the screen
                                }
                            },
                            2000
                    );   // 2000 milliseconds for 2-seconds delay

                }
            } else {    // answer was incorrect
                flagImageView.startAnimation(shakeAnimation);

                answerTextView.setText(R.string.incorrect_answer);
                answerTextView.setTextColor(Color.RED);
                guessButton.setEnabled(false);
            }
        } // end overridden onClick() method
    };  // end onClickListener

    // helper method to disable all answer Buttons
    public void disableButtons(){
        for( int row=0; row < guessRows; ++row ){
            LinearLayout guessRow = guessLinearLayouts[row];
            for(int col=0; col < guessRow.getChildCount(); ++col ){
                guessRow.getChildAt(col).setEnabled(false);
            }
        }
    }

    // method to update guessRows based on value in SharedPreferences
    public void updateGuessRows(SharedPreferences sharedPreferences) {
        // get num of guess buttons to be displayed
        // getString() uses a key (defined in MainActivity) to get a val (choices)
        String choices = sharedPreferences.getString(MainActivity.CHOICES, null);
        guessRows = Integer.parseInt(choices) / 2; // we have 2 buttons per row

        // hide all buttons LinearLayouts
        for( LinearLayout layout : guessLinearLayouts ){
            layout.setVisibility(View.GONE);
        }

        // show corresponding num of buttons user picked in prefs
        for( int row=0; row < guessRows; ++row ){
            guessLinearLayouts[row].setVisibility(View.VISIBLE);
        }
    }

    // method to update Regions based on value in SharedPreferences
    public void updateRegions(SharedPreferences sharedPreferences) {
        // get the set of regions user picked in prefs
        regionsSet = sharedPreferences.getStringSet(MainActivity.REGIONS, null);
    }

    // helper method to extra country name from filename
    private String getCountryName(String name) {
        return name.substring(name.indexOf('-') + 1).replace('_', ' ');
    }


}
