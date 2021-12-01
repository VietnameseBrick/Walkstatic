package edu.ucsd.cse110.walkstatic;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import edu.ucsd.cse110.walkstatic.runs.Run;
import edu.ucsd.cse110.walkstatic.speech.SpeechListener;
import edu.ucsd.cse110.walkstatic.speech.VoiceDictationFactory;
import edu.ucsd.cse110.walkstatic.speech.VoiceDictation;

public class EditRunFragment extends Fragment implements SpeechListener {
    private enum RunElement {
        NAME(R.id.dictate_name),
        STARTING_POINT(R.id.dictate_starting_point);

        private int buttonId;
        RunElement(int buttonId){
            this.buttonId = buttonId;
        }

        public int getButtonId(){
            return this.buttonId;
        }
    }
    private static String TYPE_KEY = "runKey";

    private VoiceDictation voiceDictation;
    private boolean isValid;
    private boolean isFavorited;

    private Run run;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setHasOptionsMenu(true);
        this.voiceDictation = VoiceDictationFactory.getVoiceDictation(this.getActivity());
        this.voiceDictation.setListener(this);
        this.isValid = false;
        this.isFavorited = false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit_run, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState){
        super.onViewCreated(view, savedInstanceState);
        this.addSpeechListeners();
        this.addValidators();
        Bundle arguments = this.getArguments();
        if(arguments != null && arguments.getSerializable("Run") != null){
            Run run = (Run)this.getArguments().getSerializable("Run");
            this.run = run;
            this.populateWithRun(run);
        } else {
            this.run = new Run();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
        inflater.inflate(R.menu.edit_run_menu, menu);
        super.onCreateOptionsMenu(menu,inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == R.id.action_save && this.isValid){
            this.saveRun();
            return true;
        } else if(item.getItemId() == R.id.action_favorite){
            this.isFavorited = !this.isFavorited;
            this.getActivity().invalidateOptionsMenu();
            return true;
        } else {
            EditText runNameElement = this.getActivity().findViewById(R.id.run_name_text);
            EditText startingPoint = this.getActivity().findViewById(R.id.starting_point_text);
            runNameElement.clearFocus();
            startingPoint.clearFocus();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu){
        super.onPrepareOptionsMenu(menu);
        MenuItem save = menu.findItem(R.id.action_save);
        save.setEnabled(this.isValid);
        int tint = this.isValid ? R.color.tintActive : R.color.tintDisabled;
        save.setIconTintList(getContext().getResources().getColorStateList(tint, null));
        MenuItem favorite = menu.findItem(R.id.action_favorite);
        int favoriteIcon = this.isFavorited ? R.drawable.ic_star_white_24dp : R.drawable.ic_star_border_white_24dp;
        favorite.setIcon(favoriteIcon);
    }

    @Override
    public void onDestroyView(){
        this.voiceDictation.cancel();
        super.onDestroyView();
    }

    private void addSpeechListeners(){
        ImageButton nameButton = this.getActivity().findViewById(R.id.dictate_name);
        nameButton.setOnClickListener(new VoiceDictationClickListener(RunElement.NAME));

        ImageButton startingPointButton = this.getActivity().findViewById(R.id.dictate_starting_point);
        startingPointButton.setOnClickListener(new VoiceDictationClickListener(RunElement.STARTING_POINT));
    }

    private class VoiceDictationClickListener implements View.OnClickListener {
        RunElement runElement;
        public VoiceDictationClickListener(RunElement runElement){
            this.runElement = runElement;
        }

        @Override
        public void onClick(View v) {
            Bundle bundle = new Bundle();
            bundle.putInt(TYPE_KEY, runElement.ordinal());
            colorMicButton(runElement, true);
            setButtonsEnabled(false);
            voiceDictation.doRecognition(bundle);
        }
    }

    @Override
    public void onSpeech(@NonNull String received, @Nullable Bundle options) {
        if(options == null){
            return;
        }
        RunElement element = RunElement.values()[options.getInt(TYPE_KEY)];
        EditText editText = null;
        if(element == RunElement.NAME) {
            editText = this.getActivity().findViewById(R.id.run_name_text);
        }
        if(element == RunElement.STARTING_POINT) {
            editText = this.getActivity().findViewById(R.id.starting_point_text);
        }
        editText.setText(received);
    }

    @Override
    public void onSpeechDone(boolean error, @Nullable Bundle options) {
        if(options == null) return;

        colorMicButton(RunElement.values()[options.getInt(TYPE_KEY)], false);
        setButtonsEnabled(true);
    }

    private void colorMicButton(RunElement element, boolean active){
        ImageButton imageButton = this.getActivity().findViewById(element.getButtonId());
        int background = R.color.micBackgroundOff;
        int tint = R.color.micOff;
        if(active){
            background = R.color.micBackgroundActive;
            tint = R.color.micActive;
        }
        imageButton.setBackgroundTintList(getContext().getResources().getColorStateList(background, null));
        imageButton.setColorFilter(getContext().getColor(tint), android.graphics.PorterDuff.Mode.SRC_IN);
    }

    private void setButtonsEnabled(boolean enabled){
        for(RunElement element : RunElement.values()){
            ImageButton button = this.getActivity().findViewById(element.getButtonId());
            button.setEnabled(enabled);
        }
    }

    private void addValidators(){
        EditText runName = this.getActivity().findViewById(R.id.run_name_text);
        runName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable editable) {
                if(TextUtils.isEmpty(editable)){
                    runName.setError(getContext().getResources().getString(R.string.name_empty_error));
                    isValid = false;
                } else {
                    isValid = true;
                    runName.setError(null);
                }
                requireActivity().invalidateOptionsMenu();
            }
        });
    }

    private void populateWithRun(Run run){
        EditText runNameElement = this.requireActivity().findViewById(R.id.run_name_text);
        EditText startingPoint = this.requireActivity().findViewById(R.id.starting_point_text);
        EditText notes = this.requireActivity().findViewById(R.id.notes);
        Spinner difficultySpinner = this.requireActivity().findViewById(R.id.difficulty_spinner);

        runNameElement.setText(run.getName());
        startingPoint.setText(run.getStartingPoint());
        notes.setText(run.getNotes());
        this.isFavorited = run.isFavorited();

        String[] difficultyArray = this.getResources().getStringArray(R.array.difficulty_array);
        List<String> difficultyList = Arrays.asList(difficultyArray);
        int difficultIndex = difficultyList.indexOf(run.getDifficulty());
        if(difficultIndex != -1){
            difficultySpinner.setSelection(difficultIndex);
        }
        this.requireActivity().invalidateOptionsMenu();
    }

    private void saveRun(){
        EditText runNameElement = this.getActivity().findViewById(R.id.run_name_text);
        EditText startingPoint = this.getActivity().findViewById(R.id.starting_point_text);
        EditText notes = this.getActivity().findViewById(R.id.notes);

        String runName = runNameElement.getText().toString();
        String runStartingPoint = startingPoint.getText().toString();
        String runNotes = notes.getText().toString();

        Spinner difficultySpinner = this.getActivity().findViewById(R.id.difficulty_spinner);
        String difficulty = difficultySpinner.getSelectedItem().toString();

        Spinner loopVsOutSpinner = this.getActivity().findViewById(R.id.loopVsOut_spinner);
        String loopVsOut = loopVsOutSpinner.getSelectedItem().toString();

        Spinner flatVsHillySpinner = this.getActivity().findViewById(R.id.flatVsHilly_spinner);
        String flatVsHilly = flatVsHillySpinner.getSelectedItem().toString();

        Spinner streetVsTrailSpinner = this.getActivity().findViewById(R.id.streetVsTrail_spinner);
        String streetVsTrail = streetVsTrailSpinner.getSelectedItem().toString();

        Spinner evenVsUnevenSpinner = this.getActivity().findViewById(R.id.evenVsUneven_spinner);
        String evenVsUneven = evenVsUnevenSpinner.getSelectedItem().toString();

        run.setName(runName);
        run.setStartingPoint(runStartingPoint);
        run.setFavorited(this.isFavorited);
        run.setDifficulty(difficulty);
        run.setflatVsHilly(flatVsHilly);
        run.setloopVsOut(loopVsOut);
        run.setstreetVsTrail(streetVsTrail);
        run.setevenVsUneven(evenVsUneven);
        run.setNotes(runNotes);

        RunViewModel runViewModel = new ViewModelProvider(this.getActivity()).get(RunViewModel.class);
        runViewModel.setRun(run);

        runNameElement.clearFocus();
        startingPoint.clearFocus();
        notes.clearFocus();
        Navigation.findNavController(this.getView()).navigateUp();
    }
}
