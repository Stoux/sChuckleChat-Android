package nl.stoux.schucklechat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

import nl.stoux.schucklechat.data.CachedData;
import nl.stoux.schucklechat.data.ConnectionHandler;
import nl.stoux.schucklechat.data.Profile;
import nl.stoux.schucklechat.json.JsonUtil;

import org.json.JSONException;
import org.json.JSONObject;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Activity which displays a login screen to the user, offering registration as
 * well.
 */
public class LoginActivity extends Activity {

	private final Context context = this;
	
	/**
	 * The default email to populate the email field with.
	 */
	public static final String EXTRA_EMAIL = "com.example.android.authenticatordemo.extra.EMAIL";

	/**
	 * Keep track of the login task to ensure we can cancel it if requested.
	 */
	private AsyncTask<Void, String, Integer> mAuthTask = null;

	// Values for email and password at the time of the login attempt.
	private String mEmail;
	private String mPassword;
	
	private boolean register = false;
	private String lastEmail = "";

	// UI references.
	private EditText mEmailView;
	private EditText mPasswordView;
	private View mLoginFormView;
	private View mLoginStatusView;
	private TextView mLoginStatusMessageView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_login);
		
		setTitle("Inloggen");

		// Set up the login form.
		mEmail = getIntent().getStringExtra(EXTRA_EMAIL);
		mEmailView = (EditText) findViewById(R.id.email);
		mEmailView.setText(mEmail);

		mPasswordView = (EditText) findViewById(R.id.password);
		mPasswordView
				.setOnEditorActionListener(new TextView.OnEditorActionListener() {
					@Override
					public boolean onEditorAction(TextView textView, int id,
							KeyEvent keyEvent) {
						if (id == R.id.login || id == EditorInfo.IME_NULL) {
							attemptLogin();
							return true;
						}
						return false;
					}
				});

		mLoginFormView = findViewById(R.id.login_form);
		mLoginStatusView = findViewById(R.id.login_status);
		mLoginStatusMessageView = (TextView) findViewById(R.id.login_status_message);

		findViewById(R.id.sign_in_button).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						attemptLogin();
					}
				});
		
		//Saved credentials
		boolean autoLogin = true;
		Bundle bundles = getIntent().getExtras();
		if (bundles != null) {
			autoLogin = !bundles.containsKey(ChatListActivity.PREVENT_AUTO_LOGIN);
		}
		
		if (autoLogin) {
			JSONObject object = JsonUtil.loadJson(context, "last-credentials.json");
			if (object != null) {
				try {
					String email = object.getString("email");
					String pass = object.getString("password");
					CachedData.lastCheck = object.getLong("last-check");
					System.out.println(CachedData.lastCheck);
					
					//Set email
					mEmailView.setText(email);
					mEmail = email;
					lastEmail = email;
					
					//Set password
					mPasswordView.setText(pass);
					mPassword = pass;
					
					//Attempt to login
					attemptLogin();
				} catch (JSONException e) {
					
				}
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.login, menu);
		return true;
	}

	/**
	 * Attempts to sign in or register the account specified by the login form.
	 * If there are form errors (invalid email, missing fields, etc.), the
	 * errors are presented and no actual login attempt is made.
	 */
	public void attemptLogin() {
		if (mAuthTask != null) {
			return;
		}

		// Reset errors.
		mEmailView.setError(null);
		mPasswordView.setError(null);

		// Store values at the time of the login attempt.
		mEmail = mEmailView.getText().toString();
		mPassword = mPasswordView.getText().toString();

		boolean cancel = false;
		View focusView = null;

		// Check for a valid password.
		if (TextUtils.isEmpty(mPassword)) {
			mPasswordView.setError(getString(R.string.error_field_required));
			focusView = mPasswordView;
			cancel = true;
		} else if (mPassword.length() < 4) {
			mPasswordView.setError(getString(R.string.error_invalid_password));
			focusView = mPasswordView;
			cancel = true;
		}

		// Check for a valid email address.
		if (TextUtils.isEmpty(mEmail)) {
			mEmailView.setError(getString(R.string.error_field_required));
			focusView = mEmailView;
			cancel = true;
		} else if (!mEmail.contains("@")) {
			mEmailView.setError(getString(R.string.error_invalid_email));
			focusView = mEmailView;
			cancel = true;
		}

		if (cancel) {
			// There was an error; don't attempt login and focus the first
			// form field with an error.
			focusView.requestFocus();
		} else {
			// Show a progress spinner, and kick off a background task to
			// perform the user login attempt.
			mLoginStatusMessageView.setText(R.string.login_progress_signing_in);
			showProgress(true);
			mAuthTask = new UserLoginTask();
			mAuthTask.execute((Void) null);
		}
	}

	/**
	 * Shows the progress UI and hides the login form.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
	private void showProgress(final boolean show) {
		// On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
		// for very easy animations. If available, use these APIs to fade-in
		// the progress spinner.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
			int shortAnimTime = getResources().getInteger(
					android.R.integer.config_shortAnimTime);

			mLoginStatusView.setVisibility(View.VISIBLE);
			mLoginStatusView.animate().setDuration(shortAnimTime)
					.alpha(show ? 1 : 0)
					.setListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(Animator animation) {
							mLoginStatusView.setVisibility(show ? View.VISIBLE
									: View.GONE);
						}
					});

			mLoginFormView.setVisibility(View.VISIBLE);
			mLoginFormView.animate().setDuration(shortAnimTime)
					.alpha(show ? 0 : 1)
					.setListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(Animator animation) {
							mLoginFormView.setVisibility(show ? View.GONE
									: View.VISIBLE);
						}
					});
		} else {
			// The ViewPropertyAnimator APIs are not available, so simply show
			// and hide the relevant UI components.
			mLoginStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
			mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
		}
	}

	/**
	 * Represents an asynchronous login/registration task used to authenticate
	 * the user.
	 */
	public class UserLoginTask extends AsyncTask<Void, String, Integer> {
		@Override
		protected Integer doInBackground(Void... params) {
			Socket s = null;
			BufferedReader reader;
			BufferedWriter writer;
						
			//Try to connect
			try {
				s = new Socket(ConnectionHandler.SERVER_IP, ConnectionHandler.SERVER_PORT);
				reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
				writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
			
				//Connected => Send credentials
				JSONObject container;
				try {
					//Create container
					container = new JSONObject();
					container.put("type", (register ? "register" : "login"));
					register = false;
					
					//Data => Credentials
					JSONObject credentials = new JSONObject();
					credentials.put("username", mEmail);
					credentials.put("password", mPassword);
					
					container.put("data", credentials);
				
					//Send credentials
					String conSt = container.toString();
					Log.d("deb", "Sending: " + conSt);
					writer.write(conSt);
					writer.newLine();
					writer.flush();
					
					Log.d("deb", "Waiting for response");
					
					//Wait for response
					String response = reader.readLine();
					s.close();
					
					//	=> Should be a JsonObject
					JSONObject responseObject = new JSONObject(response);
					String responseType = responseObject.getString("type");
					if (!responseType.equalsIgnoreCase("loginResponse")) throw new IOException(); //Should be correct response
					
					//Get data
					JSONObject returnData = responseObject.getJSONObject("data");
					int code = returnData.getInt("code");
					
					if (code == 202) {
						Profile.setUserID(returnData.getInt("user-id"));
					}
					return code;					
				} catch (IOException e) {
					publishProgress("Er is een fout opgetreden!");
					return 0;
				} catch (JSONException e) {
					publishProgress("Er is een fout opgetreden!");
					return 0;
				}
			
			} catch (Exception e) { //Failed to connect
				Log.d("deb", "Exception: "+ e);
				publishProgress("Kan niet verbinden!");
				return 0;
			} finally { //Try to close resources
				try {
					if (s != null) s.close();
				} catch (IOException e) {}
			}
		}
		
		@Override
		protected void onProgressUpdate(String... values) {
			Toast.makeText(context, values[0], Toast.LENGTH_SHORT).show();
		}

		@Override
		protected void onPostExecute(final Integer success) {
			
			//Switch the success code
			switch(success) {
			case 202: //Accepted credentials
				//Set profile
				Profile.setUsername(mEmail);
				Profile.setPassword(mPassword);
								
				//Check last credentials
				if (!mEmail.equalsIgnoreCase(lastEmail)) {
					//TODO Remove all chat files
					try {
						JSONObject obj = new JSONObject();
						obj.put("email", mEmail);
						obj.put("password", mPassword);
						obj.put("last-check", 0);
						CachedData.lastCheck = 0;
						JsonUtil.writeJson(context, obj, "last-credentials.json");
					} catch (JSONException e) {}
					
				}
				
				
				Intent chatList = new Intent(getApplicationContext(), ChatListActivity.class);
				startActivity(chatList);
				finish();
				Toast.makeText(context, "Ingelogd!", Toast.LENGTH_SHORT).show();
				break;
				
			case 403: //Forbidden to register
				Toast.makeText(context, "E-mail al ingebruik!", Toast.LENGTH_SHORT).show();
				break;
				
			case 404: //Username not found
				//=> Register?
				DialogInterface.OnClickListener dialogListener = new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						switch (which) {
						case DialogInterface.BUTTON_POSITIVE: //Yes button
							//=> Register
							mAuthTask = null;
							dialog.dismiss();
							Toast.makeText(context, "Registreren...", Toast.LENGTH_SHORT).show();
							mLoginStatusMessageView.setText(R.string.login_progress_registering);
							register = true;
							mAuthTask = new UserLoginTask();
							mAuthTask.execute((Void) null);
							break;
						case DialogInterface.BUTTON_NEGATIVE: //No Button
							//=> Dismiss
							dialog.dismiss();
							mAuthTask = null;
							showProgress(false);
							break;
						}
					}
				};
				
				DialogInterface.OnCancelListener dialogCancelListener = new DialogInterface.OnCancelListener() {
					
					@Override
					public void onCancel(DialogInterface dialog) {
						mAuthTask = null;
						showProgress(false);
					}
				};
				
				AlertDialog.Builder builder = new AlertDialog.Builder(context);
				
				builder
					.setMessage("Er bestaat niemand met die gebruikersnaam.\nWilt u zich registreren?")
					.setPositiveButton("Ja", dialogListener)
					.setNegativeButton("Nee", dialogListener)
					.setOnCancelListener(dialogCancelListener)
					.show();				
				break;
				
			case 406: //Password not accepted
				Toast.makeText(context, "Incorrect wachtwoord!", Toast.LENGTH_SHORT).show();
				mPasswordView.requestFocus();
				break;
			}
			
			if (success != 404) {
				mAuthTask = null;
				showProgress(false);
			}
		}

		@Override
		protected void onCancelled() {
			mAuthTask = null;
			showProgress(false);
		}
	}
	
}
