package nl.stoux.schucklechat;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class InputDialog {
	
	/**
	 * Show an input dialog
	 * @param context The context (The Activity)
	 * @param headerText The text of the header
	 * @param inputHint The input hint
	 * @param negativeButtonText The text on the negativeButton
	 * @param positiveButtonText The text on the positiveButton
	 * @param positiveButtonListener The onClick listener for the positiveButton
	 */
	public static void showInputDialog(Context context, String headerText, String inputHint, String negativeButtonText, String positiveButtonText, DialogInterface.OnClickListener positiveButtonListener) {
		//Create view
		View dialogView = LayoutInflater.from(context).inflate(R.layout.input_dialog, null);
		TextView headerView = (TextView) dialogView.findViewById(R.id.input_dialog_header);
		headerView.setText(headerText);
		final EditText inputView = (EditText) dialogView.findViewById(R.id.input_dialog_inputfield);
		inputView.setHint(inputHint);
		
		//Create Dialog
		AlertDialog.Builder aDialogBuilder = new AlertDialog.Builder(context);
		aDialogBuilder.setView(dialogView);
		
		aDialogBuilder
			.setNegativeButton(negativeButtonText, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			})
			.setPositiveButton(positiveButtonText, positiveButtonListener);
		
		//Create dialog
		AlertDialog dialog = aDialogBuilder.create();
		
		//Show dialog
		dialog.show();
		
		//Get button
		final Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
		positiveButton.setClickable(false);
		
		//InputView listener
		inputView.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {} //Can be ignored
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {} //Can be ignored
			
			@Override
			public void afterTextChanged(Editable s) {
				positiveButton.setClickable(s.length() > 0);
			}
		});
	}
	
	/**
	 * Get the text that has been entered in the InputField
	 * @param dialog The dialog
	 * @return The text
	 */
	public static String getInputText(DialogInterface dialog) {
		return ((EditText)((AlertDialog) dialog).findViewById(R.id.input_dialog_inputfield)).getText().toString();
	}

}
