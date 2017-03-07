package winterwell.jtwitter;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import winterwell.json.JSONArray;
import winterwell.json.JSONException;
import winterwell.json.JSONObject;
import winterwell.jtwitter.Twitter.IHttpClient;
import winterwell.jtwitter.Twitter.ITweet;

/**
 * Access the account methods: e.g. change your profile colours.
 * <p>
 * Note that user blocking/unblocking methods are in 
 * {@link Twitter_Users} -- see {@link Twitter#users()}
 * 
 * @author Daniel Winterstein
 */
@SuppressWarnings( {"rawtypes", "unchecked"} )
public class Twitter_Account {

	/**
	 * The access level is set for the application as a whole!
	 * 
	 * If you want different levels, make several applications.
	 */
	public static enum KAccessLevel {
		/** no login or invalid login */
		NONE,
		/** Read public messages */
		READ_ONLY,
		/** Read, write of public messages (but not DMs) */
		READ_WRITE,
		/** Read, write of public and private messages */
		READ_WRITE_DM
	}

	public static class Search {
		private Date createdAt;
		private Long id;
		private String query;

		public Search(Long id, Date createdAt, String query) {
			this.id = id;
			this.createdAt = createdAt;
			this.query = query;
		}

		public Date getCreatedAt() {
			return createdAt;
		}

		public Long getId() {
			return id;
		}

		public String getText() {
			return query;
		}

	}

	public static String COLOR_BG = "profile_background_color";

	public static String COLOR_LINK = "profile_link_color";

	public static String COLOR_SIDEBAR_BORDER = "profile_sidebar_border_color";
	public static String COLOR_SIDEBAR_FILL = "profile_sidebar_fill_color";
	public static String COLOR_TEXT = "profile_text_color";
	private KAccessLevel accessLevel;
	final Twitter jtwit;

	public Twitter_Account(Twitter jtwit) {
		assert jtwit.getHttpClient().canAuthenticate() : jtwit;
		this.jtwit = jtwit;
	}
	

	/**
	 * Call Twitter to fetch rate-limit info. This method is itself rate-limited!
	 *  
	 * @see IHttpClient#getRateLimits()
	 */
	public Map<String, RateLimit> getRateLimits() {
		return ((URLConnectionHttpClient)jtwit.getHttpClient()).updateRateLimits();
	}


	/**
	 * Create a new saved search.
	 * 
	 * @param query
	 *            The search query
	 * @return the new search
	 */
	public Search createSavedSearch(String query) {
		String url = jtwit.TWITTER_URL + "saved_searches/create.json";
		Map vars = InternalUtils.asMap("query", query);
		String json = jtwit.getHttpClient().post(url, vars, true);
		try {
			return makeSearch(new JSONObject(json));
		} catch (JSONException e) {
			throw new TwitterException.Parsing(json, e);
		}
	}

	/**
	 * Delete one of the user's saved searches!
	 * 
	 * @param id
	 *            The id for this search
	 * @return the deleted search
	 */
	public Search destroySavedSearch(Long id) {
		String url = jtwit.TWITTER_URL + "saved_searches/destroy/" + id
				+ ".json";
		String json = jtwit.getHttpClient().post(url, null, true);
		try {
			return makeSearch(new JSONObject(json));
		} catch (JSONException e) {
			throw new TwitterException.Parsing(json, e);
		}
	}

	/**
	 * @deprecated It seems Twitter have stopped sending this info! Expect null from this method.
	 * 
	 * @return What access level does this login have? If the login is bogus,
	 *         this will return {@link KAccessLevel#NONE}.
	 */
	public KAccessLevel getAccessLevel() {
		if (accessLevel != null)
			return accessLevel;
		try {
			verifyCredentials();
			return accessLevel;
		} catch (TwitterException.E401 e) {
			return KAccessLevel.NONE;
		}
	}

	/**
	 * @return The current user's saved searches on Twitter. Use
	 *         {@link ITweet#getText()} to retrieve the search query.
	 */
	public List<Search> getSavedSearches() {
		String url = jtwit.TWITTER_URL + "saved_searches.json";
		String json = jtwit.getHttpClient().getPage(url, null, true);
		try {
			JSONArray ja = new JSONArray(json);
			List<Search> searches = new ArrayList();
			for (int i = 0; i < ja.length(); i++) {
				final JSONObject jo = ja.getJSONObject(i);
				Search search = makeSearch(jo);
				searches.add(search);
			}
			return searches;
		} catch (JSONException e) {
			throw new TwitterException.Parsing(json, e);
		}
	}

	/**
	 * Reuse the ITweet interface. This is a bit dodgy (it's not a message), but
	 * it has exactly the methods we want.
	 * 
	 * @param jo
	 * @return a search in ITweet format.
	 * @throws JSONException
	 */
	private Search makeSearch(JSONObject jo) throws JSONException {
		final Date createdAt = InternalUtils.parseDate(jo
				.getString("created_at"));
		final Long id = jo.getLong("id");
		final String query = jo.getString("query");
		Search search = new Search(id, createdAt, query);
		return search;
	}

	/**
	 * Update profile.
	 * 
	 * @param name
	 *            Can be null for no change. Full name associated with the
	 *            profile. Maximum of 20 characters.
	 * @param url
	 *            Can be null for no change. URL associated with the profile.
	 *            Will be prepended with "http://" if not present. Maximum of
	 *            100 characters.
	 * @param location
	 *            Can be null for no change. The city or country describing
	 *            where the user of the account is located. The contents are not
	 *            normalized or geocoded in any way. Maximum of 30 characters.
	 * @param description
	 *            Can be null for no change. A description of the user. Maximum
	 *            of 160 characters.
	 * @return updated User object
	 */
	public User setProfile(String name, String url, String location,
			String description) {
		Map<String, String> vars = InternalUtils.asMap("name", name, "url",
				url, "location", location, "description", description);
		String apiUrl = jtwit.TWITTER_URL + "/account/update_profile.json";
		String json = jtwit.getHttpClient().post(apiUrl, vars, true);
		return InternalUtils.user(json);
	}
	
	/**
	 * Convenience method for {@link #setProfile(String, String, String, String)}
	 * @param description
	 */
	public User setProfileDescription(String description) {
		return setProfile(null, null, null, description);
	}
	/**
	 * Convenience method for {@link #setProfile(String, String, String, String)}
	 * @param description
	 */
	public User setProfileLink(String url) {
		return setProfile(null, url, null, null);
	}
	/**
	 * Convenience method for {@link #setProfile(String, String, String, String)}
	 * @param description
	 */
	public User setProfileLocation(String locn) {
		return setProfile(null, null, locn, null);
	}
	

	/**
	 * See https://dev.twitter.com/rest/reference/post/account/update_profile_image.
	 * Note: changes may take 5 seconds to be processed.
	 * 
	 * @param image gif, jpg, or png <700k. Larger images will be scaled by Twitter.
	 * Use a square image! Otherwise Twitter will crop it ruthlessly.
	 * @return User
	 */
	public User setProfileImage(File image) {
		Map<String, String> vars = InternalUtils.asMap("image",image);
		String url = jtwit.TWITTER_URL+"/account/update_profile_image.json";
		String result = ((OAuthSignpostClient)jtwit.getHttpClient()).postMultipartForm(url, vars);
		return InternalUtils.user(result);
	}

	/**
	 * See https://dev.twitter.com/rest/reference/post/account/update_profile_banner
	 * 
	 * NB: The API supports cropping, but we have not implemented this yet.
	 * @param image
	 * @return the updated user
	 */
	public User setProfileBanner(File image) {
		Map<String, String> vars = InternalUtils.asMap("banner",image);
		String url = jtwit.TWITTER_URL+"/account/update_profile_banner.json";
		String result = ((OAuthSignpostClient)jtwit.getHttpClient()).postMultipartForm(url, vars);
		return InternalUtils.user(result);
	}

	/**
	 */
	public User removeProfileBanner() {
		String url = jtwit.TWITTER_URL+"/account/remove_profile_banner.json";
		String result = ((OAuthSignpostClient)jtwit.getHttpClient()).post(url, new HashMap(), true);
		return InternalUtils.user(result);
	}

	/**
	 * Set the authenticating user's colors.
	 * 
	 * @param colorName2hexCode
	 *            Use the COLOR_XXX constants as keys, and 3 or 6 letter
	 *            hex-codes as values (e.g. 0f0 or 00ff00 both code for green).
	 *            You can set as many colors as you like (but at least one).
	 * @return updated User object
	 */
	public User setProfileColors(Map<String, String> colorName2hexCode) {
		assert colorName2hexCode.size() != 0;
		String url = jtwit.TWITTER_URL + "/account/update_profile_colors.json";
		String json = jtwit.getHttpClient().post(url, colorName2hexCode, true);
		return InternalUtils.user(json);
	}

	@Override
	public String toString() {
		return "TwitterAccount[" + jtwit.getScreenName() + "]";
	}

	/**
	 * Test the login credentials -- and get some user info (which gets cached
	 * at {@link Twitter#getSelf()}).
	 * 
	 * @return a representation of the requesting user if authentication was
	 *         successful
	 * @throws TwitterException.E401
	 *             thrown if the authorisation credentials fail.
	 * 
	 * @see Twitter#isValidLogin()
	 */
	public User verifyCredentials() throws TwitterException.E401 {
		String url = jtwit.TWITTER_URL + "/account/verify_credentials.json";
		String json = jtwit.getHttpClient().getPage(url, null, true);
		// store the access level info
		IHttpClient client = jtwit.getHttpClient();
		String al = client.getHeader("X-Access-Level");
		if (al != null) {
			if ("read".equals(al)) {
				accessLevel = KAccessLevel.READ_ONLY;
			}
			if ("read-write".equals(al)) {
				accessLevel = KAccessLevel.READ_WRITE;
			}
			if ("read-write-directmessages".equals(al)) {
				accessLevel = KAccessLevel.READ_WRITE_DM;
			}
		}
		User self = InternalUtils.user(json);
		// update the self object
		jtwit.self = self;
		return self;
	}

}
