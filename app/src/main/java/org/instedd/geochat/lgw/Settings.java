package org.instedd.geochat.lgw;

import org.instedd.geochat.lgw.msg.Country;
import org.instedd.geochat.lgw.msg.NuntiumClient;
import org.instedd.geochat.lgw.msg.NuntiumClientException;
import org.instedd.geochat.lgw.msg.NuntiumTicket;
import org.instedd.geochat.lgw.msg.QstClient;
import org.instedd.geochat.lgw.msg.RestClient;
import org.instedd.geochat.lgw.msg.WrongHostException;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.text.TextUtils;

public class Settings implements ISettings {

	public final static String SHARED_PREFS_NAME = "org.instedd.geochat.lgw.settings";
	public final static String REFRESH_RATE_SECONDS = "refreshRateSeconds";
	public final static String ENDPOINT_URL = "endpointUrl";
	public final static String NAME = "userName";
	public final static String PASSWORD = "userPassword";
	public final static String COUNTRY_CODE = "countryCode";
	public final static String NUMBER = "telephoneNumber";
	public final static String LAST_RECEIVED_MESSAGE_ID = "lastSentMessageId";
	public final static String ADD_PLUS_TO_OUTGOING = "addPlusToOutgoing";
	public final static String MAX_MESSAGE_AGE_IN_DAYS = "maxMessageAge";
	public final static String WAIT_BETWEEN_MESSAGES_IN_SECONDS = "waitBetweenMessages";

	private final Context context;
	private NuntiumClient nuntiumClient;

	public Settings(Context context) {
		this.context = context;
	}	

	public String storedUserName() {
		return openRead().getString(NAME, null);
	}

	public String storedPassword() {
		return openRead().getString(PASSWORD, null);
	}

	public String storedTelephoneNumber() {
		return openRead().getString(NUMBER, null);
	}

	public String storedLastReceivedMessageId() {
		return openRead().getString(LAST_RECEIVED_MESSAGE_ID, null);
	}
	
	public Boolean storedAddPlusToOutgoing() {
		return openRead().getBoolean(ADD_PLUS_TO_OUTGOING, true);
	}

	public String storedEndpointUrl() {
		return openRead().getString(ENDPOINT_URL, defaultEndpointUrl());
	}

	private String defaultEndpointUrl() {
		return "http://nuntium.instedd.org/";
	}

	public int storedRefreshRateInSeconds() {
		return Integer.parseInt(openRead().getString(REFRESH_RATE_SECONDS, "60"));
	}

	public int storedRefreshRateInMilliseconds() {
		return 1000 * storedRefreshRateInSeconds();
	}

	public String storedCountryCode() {
		return openRead().getString(COUNTRY_CODE, null);
	}

	public int storedMaxMessageAgeInDays() { return Integer.parseInt(openRead().getString(MAX_MESSAGE_AGE_IN_DAYS, "7")); }

	public int storedWaitBetweenMessagesInSeconds() { return Integer.parseInt(openRead().getString(WAIT_BETWEEN_MESSAGES_IN_SECONDS, "0")); }

	public void saveLastReceivedMessageId(String id) {
		Editor editor = openWrite();
		editor.putString(LAST_RECEIVED_MESSAGE_ID, id);
		editor.commit();
	}

	public void saveCredentials(String endpointUrl, String name, String password) {
		Editor editor = openWrite();
		editor.putString(ENDPOINT_URL, endpointUrl);
		editor.putString(NAME, name);
		editor.putString(PASSWORD, password);
		editor.commit();
	}

	public void saveCountryCode(String countryCode) {
		Editor editor = openWrite();
		editor.putString(COUNTRY_CODE, countryCode);
		editor.commit();
	}

	public void saveTelephoneNumber(String number) {
		Editor editor = openWrite();
		editor.putString(NUMBER, number);
		editor.commit();
	}

	private SharedPreferences openRead() {
		return context.getSharedPreferences(SHARED_PREFS_NAME,
				Context.MODE_PRIVATE);
	}

	private Editor openWrite() {
		return openRead().edit();
	}

	public QstClient qstClient() {
		return new QstClient(context, storedEndpointUrl(), storedUserName(),
				storedPassword(), restClient(), storedCountryCode(), this);
	}

	public NuntiumClient nuntiumClient() {
		if (nuntiumClient == null) {
			nuntiumClient = new NuntiumClient(context, restClient(),
					storedEndpointBaseUrl());
		}

		return nuntiumClient;
	}

	private RestClient restClient() {
		return new RestClient(context);
	}

	public String storedEndpointBaseUrl() {
		return trimToHost(storedEndpointUrl());
	}

	private String trimToHost(String url) {
		String[] splittedUrl = url.split("/");
		if (splittedUrl.length >= 3) {
			return splittedUrl[2];
		} else {
			return "";
		}
	}

	public boolean areIncomplete() {
		checkBackwardCompatibleSettings();
		
		return isEndpointUrlUnset() || isUserNameUnset() || isPasswordUnset()
				|| isTelephoneNumberUnset() || isCountryCodeUnset();
	}

	private boolean isCountryCodeUnset() {
		return TextUtils.isEmpty(storedCountryCode());
	}

	private boolean isTelephoneNumberUnset() {
		return TextUtils.isEmpty(storedTelephoneNumber());
	}

	private boolean isPasswordUnset() {
		return TextUtils.isEmpty(storedPassword());
	}

	private boolean isUserNameUnset() {
		return TextUtils.isEmpty(storedUserName());
	}

	private boolean isEndpointUrlUnset() {
		return TextUtils.isEmpty(storedEndpointUrl());
	}

	public int storedCountryCodeIndex() throws NuntiumClientException,
			WrongHostException {
		String storedCountryCode = storedCountryCode();
		Country[] countries = nuntiumClient().countries();

		for (int i = 0; i < countries.length; i++) {
			if (countries[i].getPhonePrefix().equals(storedCountryCode))
				return i;
		}
		return -1;
	}

	public void saveCountryCodeAtIndex(int position)
			throws NuntiumClientException, WrongHostException {
		saveCountryCode(nuntiumClient().countries()[position].getPhonePrefix());
	}

	public void saveCredentialsFrom(NuntiumTicket nuntiumTicket) {
		saveCredentials(endpointUrlFrom(nuntiumTicket), nuntiumTicket.data()
				.get("channel"), nuntiumTicket.data().get("password"));
	}

	private String endpointUrlFrom(NuntiumTicket nuntiumTicket) {
		return "http://" + storedEndpointBaseUrl() + "/"
				+ nuntiumTicket.data().get("account") + "/qst";
	}

	public void resetAdvancedConfiguration() {
		saveCredentials(defaultEndpointUrl(), "", "");
	}
	
	private void checkBackwardCompatibleSettings() {
		String oldName = openRead().getString("name", null);
		if (oldName != null) {
			String oldPassword = openRead().getString("password", null);
			
			Editor editor = openWrite();
			editor.putString(NAME, oldName);
			editor.putString(PASSWORD, oldPassword);
			editor.remove("name");
			editor.remove("password");
			editor.commit();
		}
		
		String oldCountryCode = openRead().getString("country_code", null);
		if (oldCountryCode != null) {
			Editor editor = openWrite();
			editor.putString(COUNTRY_CODE, oldCountryCode);
			editor.remove("country_code");
			editor.commit();
		}
		
		String oldTelephoneNumber = openRead().getString("number", null);
		if (oldTelephoneNumber != null) {
			Editor editor = openWrite();
			editor.putString(NUMBER, oldTelephoneNumber);
			editor.remove("number");
			editor.commit();
		}
	}
}
