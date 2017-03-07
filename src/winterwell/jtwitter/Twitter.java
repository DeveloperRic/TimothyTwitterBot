package winterwell.jtwitter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import com.winterwell.jgeoplanet.BoundingBox;
import com.winterwell.jgeoplanet.IPlace;
import com.winterwell.jgeoplanet.Location;

import winterwell.json.JSONArray;
import winterwell.json.JSONException;
import winterwell.json.JSONObject;
import winterwell.jtwitter.TwitterException.E401;
import winterwell.jtwitter.TwitterException.E403;
import winterwell.jtwitter.TwitterException.E404;
import winterwell.jtwitter.TwitterException.SuspendedUser;
import winterwell.jtwitter.ecosystem.TwitLonger;

/**
 * Java wrapper for the Twitter API version {@value #version}
 * <p>
 * Example usage:<br>
 * First, you should get the user to authorise access via OAuth. There are a
 * couple of ways of doing this -- we show one below -- see
 * {@link OAuthSignpostClient} for more details.
 * 
 * <code><pre>
	// First, OAuth to login: Make an oauth client
	OAuthSignpostClient oauthClient = new OAuthSignpostClient(JTWITTER_OAUTH_KEY, JTWITTER_OAUTH_SECRET, "oob");
    // open the authorisation page in the user's browser
    oauthClient.authorizeDesktop(); // Note: this only works on desktop PCs
    // or direct the user to the webpage given jby oauthClient.authorizeUrl()
    // get the pin from the user since we're using "oob" instead of a callback servlet
    String v = oauthClient.askUser("Please enter the verification PIN from Twitter");
    oauthClient.setAuthorizationCode(v);
	// You can store the authorisation token details for future use
    Object accessToken = client.getAccessToken();
</pre></code>
 * 
 * Now we can access Twitter: <code><pre>
	// Make a Twitter object
	Twitter twitter = new Twitter("my-name", oauthClient);
	// Print Winterstein's status
	System.out.println(twitter.getStatus("winterstein"));
	// Set my status
	twitter.updateStatus("Messing about in Java");
</pre></code>
 * 
 * <p>
 * If you can handle callbacks, then the OAuth login can be streamlined. You
 * need a webserver and a servlet (eg. use Jetty or Tomcat) to handle callbacks.
 * Replace "oob" with your callback url. Direct the user to
 * client.authorizeUrl(). Twitter will then call your callback with the request
 * token and verifier (authorisation code).
 * </p>
 * <p>
 * See {@link http://www.winterwell.com/software/jtwitter.php} for more
 * information about this wrapper. See {@link http://dev.twitter.com/doc} for
 * more information about the Twitter API.
 * <p>
 * Notes:
 * <ul>
 * <li>Takes care of all url-encoding/decoding.
 * <li>Will throw a runtime exception (TwitterException) if a
 * methods fails, e.g. it cannot connect to Twitter.com or you make a bad
 * request.
 * <li>Note that Twitter treats old-style retweets (those made by sending a
 * normal tweet beginning "RT @whoever") differently from new-style retweets
 * (those made using the retweet API). The differences are documented in various
 * methods.
 * <li>This class itself holds tweet-related methods.
 * Also see the "sub-classes" accessed via {@link #users()}, {@link #geo()}, and {@link #account()}. 
 * For list support see {@link TwitterList} - though {@link #getLists()} is here.
 * <li>This class is NOT thread safe. If you're using multiple threads, it is
 * best to create separate Twitter objects (which is fine).
 * </ul>
 * 
 * <h4>Copyright and License</h4>
 * This code is copyright (c) Winterwell Associates 2008/2009 and (c) winterwell
 * Mathematics Ltd, 2007 except where otherwise stated. It is released as
 * open-source under the LGPL license. See <a
 * href="http://www.gnu.org/licenses/lgpl.html"
 * >http://www.gnu.org/licenses/lgpl.html</a> for license details. This code
 * comes with no warranty or support.
 * 
 * <h4>Change List</h4>
 * The change list is kept online at: {@link http
 * ://www.winterwell.com/software/changelist.txt}
 * 
 * @author Daniel Winterstein
 */
@SuppressWarnings( {"rawtypes", "unchecked", "unused"} )
public class Twitter implements Serializable {
	/**
	 * Use to register per-page callbacks for long-running searches. To stop the
	 * search, return true.
	 * 
	 */
	public interface ICallback {
		public boolean process(List<Status> statuses);
	}
			
	/**
	 * How is the Twitter API today?
	 * See {@link https://dev.twitter.com/status} for more information. 
	 * @return map of {method: %uptime in the last 24 hours}.
	 * An empty map indicates this method itself failed!
	 * 
	 * @throws Exception This method is not officially supported! As such,
	 * it could break at some future point.
	 */
	public static Map<String,Double> getAPIStatus() throws Exception{
		HashMap<String,Double> map = new HashMap();
		// c.f. https://dev.twitter.com/status & https://status.io.watchmouse.com/7617
		// https://api.io.watchmouse.com/synth/current/39657/folder/7617/?fields=info;cur;24h.uptime;24h.status;last.date;daily.avg;daily.uptime;daily.status;daily.period
		String json = null;
		try {
			URLConnectionHttpClient client = new URLConnectionHttpClient();
			json = client.getPage("https://api.io.watchmouse.com/synth/current/39657/folder/7617/?fields=info;cur;24h.uptime", null, false);
			JSONObject jobj = new JSONObject(json);
			JSONArray jarr = jobj.getJSONArray("result");
			for(int i=0; i<jarr.length(); i++) {
				JSONObject jo = jarr.getJSONObject(i);
				String name = jo.getJSONObject("info").getString("name");
				JSONObject h24 = jo.getJSONObject("24h");
				double value = h24.getDouble("uptime");
				map.put(name, value);
			}
			return map;
		} catch (JSONException e) {
			throw new TwitterException.Parsing(json, e);
		} catch (Exception e) {
			return map;
		}		
	}
	
	/**
	 * Interface for an http client - e.g. allows for OAuth to be used instead.
	 * The standard version is {@link OAuthSignpostClient}.
	 * <p>
	 * If creating your own version, please provide support for throwing the
	 * right subclass of TwitterException - see
	 * {@link URLConnectionHttpClient#processError(java.net.HttpURLConnection)}
	 * for example code.
	 * 
	 * @author Daniel Winterstein
	 */
	public static interface IHttpClient {

		/**
		 * Whether this client is setup to do authentication when contacting the
		 * Twitter server. Note: This is a fast method that does not call the
		 * server, so it does not check whether the access token or password is
		 * valid. See {Twitter#isValidLogin()} or
		 * {@link Twitter_Account#verifyCredentials()} if you need to check a
		 * login.
		 * */
		boolean canAuthenticate();

		/**
		 * Lower-level GET method.
		 * 
		 * @param url
		 * @param vars
		 * @param authenticate
		 * @return
		 * @throws IOException
		 */
		HttpURLConnection connect(String url, Map<String, String> vars,
				boolean authenticate) throws IOException;

		/**
		 * @return a copy of this client. The copy can share structure, but it
		 *         MUST be safe for passing to a new thread to be used in
		 *         parallel with the original.
		 */
		IHttpClient copy();

		/**
		 * Fetch a header from the last http request. This is inherently NOT
		 * thread safe. Headers from error messages should (probably) be cached.
		 * 
		 * @param headerName
		 * @return header value, or null if unset
		 */
		String getHeader(String headerName);

		/**
		 * Send an HTTP GET request and return the response body. Note that this
		 * will change all line breaks into system line breaks!
		 * 
		 * @param uri
		 *            The uri to fetch
		 * @param vars
		 *            get arguments to add to the uri
		 * @param authenticate
		 *            If true, use authentication. The authentication method
		 *            used depends on the implementation (basic-auth, OAuth). It
		 *            is an error to use true if no authentication details have
		 *            been set.
		 * 
		 * @throws TwitterException
		 *             for a variety of reasons
		 * @throws TwitterException.E404
		 *             for resource-does-not-exist errors
		 */
		String getPage(String uri, Map<String, String> vars,
				boolean authenticate) throws TwitterException;

		/**
		 * @deprecated
		 * @see #getRateLimits() 
		 */
		RateLimit getRateLimit(KRequestType reqType);
		
		/**
		 * @return resource-to-rate-limit info, if known, or null.
		 * This method does _not_ perform a Twitter call.
		 */
		Map<String, RateLimit> getRateLimits();
					
		/**
		 * Send an HTTP POST request and return the response body.
		 * 
		 * @param uri
		 *            The uri to post to.
		 * @param vars
		 *            The form variables to send. These are URL encoded before
		 *            sending.
		 * @param authenticate
		 *            If true, send user authentication
		 * @return The response from the server.
		 * 
		 * @throws TwitterException
		 *             for a variety of reasons
		 * @throws TwitterException.E404
		 *             for resource-does-not-exist errors
		 */
		String post(String uri, Map<String, String> vars, boolean authenticate)
				throws TwitterException;

		/**
		 * Lower-level POST method.
		 * 
		 * @param uri
		 * @param vars
		 * @return a freshly opened authorised connection
		 * @throws TwitterException
		 */
		HttpURLConnection post2_connect(String uri, Map<String, String> vars)
				throws Exception;

		/**
		 * Set the timeout for a single get/post request. This is an optional
		 * method - implementations can ignore it!
		 * 
		 * @param millisecs
		 */
		void setTimeout(int millisecs);

		/**
		 * If true, will wait 1/2 second and make a 2nd request when presented with
		 * a server error (E50X). Only retries once -- a 2nd fail will throw an exception.
		 * 
		 * This policy handles most Twitter server glitches.
		 */
		boolean isRetryOnError();

		void setRetryOnError(boolean retryOnError);

	}

	/**
	 * This gives common access to features that are common to both
	 * {@link Message}s and {@link Status}es.
	 * 
	 * @author daniel
	 * 
	 */
	public static interface ITweet extends Serializable {

		Date getCreatedAt();

		/**
		 * Twitter IDs are numbers - but they can exceed the range of Java's
		 * signed long.
		 * 
		 * @return The Twitter id for this post. This is used by some API
		 *         methods.
		 */
		BigInteger getId();

		/**
		 * @return the location of this tweet. Can be null, never blank. This
		 *         can come from geo-tagging or the user's location. This may be
		 *         a place name, or in the form "latitude,longitude" if it came
		 *         from a geo-tagged source.
		 *         <p>
		 *         Note: This will be set if Twitter supply any geo-information.
		 *         We extract a location from geo and place objects.
		 */
		String getLocation();

		/**
		 * @return list of screen-names this message is to. May be empty, never
		 *         null. For Statuses, this is anyone mentioned in the message.
		 *         For DMs, this is a wrapper round
		 *         {@link Message#getRecipient()}.
		 *         <p>
		 *         Notes: This method is in ITweet as a convenience to allow the
		 *         same code to process both Statuses and Messages where
		 *         possible. It would be better named "getRecipients()", but for
		 *         historical reasons it isn't.
		 */
		List<String> getMentions();

		/**
		 * @return more information on the location of this tweet. This is
		 *         usually null!
		 */
		Place getPlace();

		/** The actual status text. This is also returned by {@link #toString()} */
		String getText();

		/**
		 * Twitter wrap urls with their own url-shortener (as a defence against
		 * malicious tweets). You are recommended to direct people to the
		 * Twitter-url, but use the original url for display.
		 * <p>
		 * Entity support is off by default. Request entity support by setting
		 * {@link Twitter#setIncludeTweetEntities(boolean)}. Twitter do NOT
		 * support entities for search :(
		 * 
		 * @param type
		 *            urls, user_mentions, or hashtags
		 * @return the text entities in this tweet, or null if the info was not
		 *         supplied.
		 */
		List<TweetEntity> getTweetEntities(KEntityType type);

		/** The User who made the tweet */
		User getUser();

		/**
		 * @return text, with the t.co urls replaced.
		 * Use-case: for filtering based on text contents, when we want to
		 * match against the full url.
		 * Note: this does NOT resolve short urls from bit.ly etc. 
		 */
		String getDisplayText();

	}

	public static enum KEntityType {
		hashtags, urls, user_mentions, media, symbols, extended_entities
	}

	/**
	 * @deprecated Replaced in 1.1 with a more flexible family of resources.
	 * 
	 * Kept here for backwards compatibility only.
	 * Will be removed: June 2013!
	 */
	public static enum KRequestType {
		NORMAL(RateLimit.RES_USER_TIMELINE), 
		SEARCH(RateLimit.RES_SEARCH),
		/** this is X-Feature Class "namesearch" in the response headers */
		SEARCH_USERS("/users/search"), 
		SHOW_USER(RateLimit.RES_USERS_SHOW1), 
		UPLOAD_MEDIA("Media"),
		STREAM_KEYWORD(""),
		STREAM_USER("");

		/**
		 * USed to find the X-?RateLimit header.
		 */
		final String rateLimit;

		private KRequestType(String rateLimit) {
			this.rateLimit = rateLimit;
		}
	}

	/**
	 * A special slice of text within a tweet.
	 * 
	 * @see Twitter#setIncludeTweetEntities(boolean)
	 */
	public final static class TweetEntity implements Serializable {
		private static final long serialVersionUID = 1L;

		/**
		 * 
		 * @param tweet
		 * @param rawText
		 * @param type
		 * @param jsonEntities
		 * @return Can be null if no entities of this type are specified
		 * @throws JSONException
		 */
		static ArrayList<TweetEntity> parse(ITweet tweet, String rawText, KEntityType type,
				JSONObject jsonEntities) throws JSONException 
		{
			assert type != null && tweet != null && rawText != null && jsonEntities!=null
								: tweet+"\t"+rawText+"\t"+type+"\t"+jsonEntities;
			try {
				JSONArray arr = jsonEntities.optJSONArray(type.toString());
				// e.g. "user_mentions":[{"id":19720954,"name":"Lilly Hunter","indices":[0,10],"screen_name":"LillyLyle"}
				if (arr==null || arr.length()==0) {
					return null;
				}
				ArrayList<TweetEntity> list = new ArrayList<TweetEntity>(
						arr.length());
				for (int i = 0; i < arr.length(); i++) {
					JSONObject obj = arr.getJSONObject(i);
					TweetEntity te = new TweetEntity(tweet, rawText, type, obj, list);
					list.add(te);
				}
				return list;
			} catch (Throwable e) {
				// whatever bogus data Twitter send, don't fail
				return null;
			}
		}

		final String display;
		/**
		 * end of the entity in the contents String, exclusive
		 */
		public final int end;
		/**
		 * start of the entity in the contents String, inclusive
		 */
		public final int start;
		private final ITweet tweet;

		public final KEntityType type;
		
		/**
		 * Location of the actual image file (if there is one) - used when getting attached images from DMs
		 */
		final String mediaUrl;

		/**
		 * 
		 * @param tweet
		 * @param rawText Needed to undo the indexing errors created by entity encoding
		 * @param type
		 * @param obj
		 * @param previous Used to handle repeated entities
		 * @throws JSONException
		 */
		@SuppressWarnings("incomplete-switch")
		TweetEntity(ITweet tweet, String rawText, KEntityType type, JSONObject obj, ArrayList<TweetEntity> previous)
				throws JSONException 
		{
			this.tweet = tweet;
			this.type = type;
			switch (type) {
			case urls:
				Object eu = obj.opt("expanded_url");
				display = JSONObject.NULL.equals(eu) ? null : (String) eu;
				break;
			case user_mentions:
				display = obj.getString("name");
				break;
			default:
				display = null;
			}
			
			// Init mediaUrl
			if ( KEntityType.media.equals(this.type)) {
				this.mediaUrl = obj.getString("media_url");
			} else {
				this.mediaUrl = null;
			}
			
			// start, end
			JSONArray indices = obj.getJSONArray("indices");
			int _start = indices.getInt(0);
			int _end = indices.getInt(1);
			assert _start >= 0 && _end >= _start : obj;
			// Sadly, due to entity encoding, start/end may be off!
			String text = tweet.getText();
			if (rawText.regionMatches(_start, text, _start, _end - _start)) {
				// normal case: all OK			
				start = _start; end = _end;
				return;
			}
			// oh well - let's correct start/end
			// Note: This correction can go wrong in a particular case: 
			// encoding has messed up the indices & we have a repeated entity.
			// ??Do we care enough to fix such a rare corner case with moderately harmless side-effects?
			
			// Protect against (rare) dud data from Twitter
			_end = Math.min(_end, rawText.length());
			_start = Math.min(_start, _end);
			if (_start == _end) { // paranoia -- but it happens (last seen Oct 2012; see TwitterTest)
				// Guess blindly by type!
				switch(type) {
				case hashtags:
					break;
				case urls:
					Matcher m = Regex.VALID_URL.matcher(text);
					if (m.find()) {
						start = m.start();
						end = m.end();
						return;
					}
					break;
				case user_mentions:
					break;
				}
				// Fail
				end = Math.min(_end, text.length());
				start = Math.min(_start, end);
				return;
			}
				
			String entityText = rawText.substring(_start, _end);
			// Handle repeated entities -- eg same url / @name twice at different positions
			int from = 0;
			for(TweetEntity prev : previous) {
				if (tweet.getText().regionMatches(prev.start, entityText, 0, entityText.length())) {
					from = prev.end;
				}
			}
			// Find where the referenced text is in the un-encoded version
			int i = text.indexOf(entityText, from);
			if (i==-1) {
				// This can't legitimately happen, but handle it anyway 'cos it does (rare & random)
				entityText = InternalUtils.unencode(entityText);
				i = text.indexOf(entityText);
				if (i==-1) i = _start; // give up gracefully
			}
			start = i; 
			end = start + _end - _start;
		}

		/**
		 * Constructor for when you know exactly what you want (rare).
		 */
		TweetEntity(ITweet tweet, KEntityType type, int start, int end, String display) {
			this.tweet = tweet;
			this.end = end;
			this.start = start;
			this.type = type;			
			this.display = display;
			this.mediaUrl = null;
		}

		/**
		 * @return For a url: the expanded version For a user-mention: the
		 *         user's name
		 */
		public String displayVersion() {
			return display == null ? toString() : display;
		}
		
		public String mediaUrl() {
			return mediaUrl;
		}

		/**
		 * The slice of text in the tweet. E.g. for a url, this will be the
		 * *shortened* version.
		 * 
		 * @see #displayVersion()
		 */
		@Override
		public String toString() {
			// There is a strange bug where -- rarely -- end > tweet length!
			// I think this is now fixed (it was an encoding issue).
			String text = tweet.getText();
			int e = Math.min(end, text.length());
			int s = Math.min(start, e);
			return text.substring(s, e);
		}
	}

	/**
	 * This rather dangerous global toggle switches off lower-casing on Twitter
	 * screen-names.
	 * <p>
	 * Screen-names are case insensitive as far as Twitter is concerned. However
	 * you might want to preserve the case people use for display purposes.
	 * <p>
	 * false by default.
	 */
	public static boolean CASE_SENSITIVE_SCREENNAMES;

	/**
	 * This global toggle switches on/off length-checking for tweets.
	 * <p>
	 * To avoid wasting time or API rate-limit usage, JTwitter can check that outgoing
	 * tweets meet the maximum-length restriction. Set this to false to disable that
	 * check (Twitter will still apply their own check!). 
	 * <p>
	 * true by default.
	 * @see #countCharacters(String)
	 */
	public static boolean CHECK_TWEET_LENGTH = true;

	/**
	 * The length of a url after t.co shortening. Currently 23 characters.
	 * (Used to be 22 for HTTP / 23 for HTTPS but now 23 for all)
	 * <p>
	 * Use updateConfiguration()if you want to get the latest settings from
	 * Twitter.
	 */
	public static int LINK_LENGTH = 23;
	
	/**
	 * The characters used up by an attached image. Currently 23 characters (ie = an https link).
	 * <p>
	 * Use updateConfiguration()if you want to get the latest settings from
	 * Twitter.
	 */
	public static int MEDIA_LENGTH = 23;

	public static long PHOTO_SIZE_LIMIT = 3145728L; // 3mb

	public static final String SEARCH_MIXED = "mixed";

	public static final String SEARCH_POPULAR = "popular";

	/**
	 * return the most recent results in the response
	 */
	public static final String SEARCH_RECENT = "recent";

	private static final long serialVersionUID = 1L;
	/**
	 * JTwitter version
	 */
	public final static String version = "3.3.0";

	/**
	 * The maximum number of characters that a tweet can contain.
	 */
	public final static int MAX_CHARS = 140;

	/** Which version of Twitter API?
	 * The upgrade to v1.1 implemented here is necessary as of March 2013 */
	static final String API_VERSION = "1.1";

	static final String DEFAULT_TWITTER_URL = "https://api.twitter.com/"+API_VERSION;
	
	static final String TWITTER_UPLOAD_URL = "https://upload.twitter.com/" + API_VERSION;

	/**
	 * Uploaded media files will be available for use for 60 minutes before they are flushed from the servers (if not associated with a Tweet or Card).
	 */
	static final String MEDIA_UPLOAD_ENDPOINT = "/media/upload.json";

	public static final int MAX_DM_LENGTH = 10000;

	/**
	 * @deprecated Not used at present
	 * Set to true to perform extra error-handling & correction.
	 */
	public static boolean WORRIED_ABOUT_TWITTER = false;

	/**
	 * Convenience method: Finds a user with the given screen-name from the
	 * list.
	 * 
	 * @param screenName
	 *            aka login name
	 * @param users
	 * @return User with the given name, or null.
	 */
	public static User getUser(String screenName, List<User> users) {
		assert screenName != null && users != null;
		for (User user : users) {
			if (screenName.equals(user.screenName))
				return user;
		}
		return null;
	}

	/**
	 * 
	 * @param args
	 *            Can be used as a command-line tweet tool. To do so, enter 3
	 *            arguments: name, password, tweet
	 * 
	 *            If empty, prints version info.
	 */
	public static void main(String[] args) {
		// Post a tweet if we are handed a name, password and tweet
		if (args.length == 3) {
			Twitter tw = new Twitter(args[0], args[1]);
			// int s = 0;
			// List<Long> fids = tw.getFollowerIDs();
			// for (Long fid : fids) {
			// User f = tw.follow(""+fid);
			// if (f!=null) s++;
			// }
			Status s = tw.setStatus(args[2]);
			System.out.println(s);
			return;
		}
		System.out.println("Java interface for Twitter");
		System.out.println("--------------------------");
		System.out.println("Version " + version);
		System.out.println("Released under LGPL by Winterwell Associates Ltd.");
		System.out
				.println("See source code, JavaDoc, or http://winterwell.com for details on how to use.");
	}

	/**
	 * TODO merge with {@link #maxResults}??
	 */
	Integer count;

	/**
	 * Used by search
	 */
	private String geocode;
	final IHttpClient http;

	boolean includeRTs = true;

	private String lang;


	/**
	 * Provides support for fetching many pages of results.
	 * -1 = 1 page's worth
	 */
	private int maxResults = -1;

	private double[] myLatLong;

	/**
	 * Twitter login name. Can be null even if we have authentication when using
	 * OAuth.
	 */
	private String name;

	private String resultType;

	/**
	 * The user. Can be null. Can be a "fake-user" (screenname-only) object.
	 */
	User self;

	private Date sinceDate;

	private BigInteger sinceId;

	private String sourceApp = "jtwitterlib";

	boolean tweetEntities = true;

	@Deprecated // Keeping for backwards compatibility of serialised form until Q2 2013
	private transient String twitlongerApiKey;

	@Deprecated // Keeping for backwards compatibility of serialised form until Q2 2013
	private transient String twitlongerAppName;

	/**
	 * E.g. "https://api.twitter.com/1.1"<br>
	 * 
	 * Change this to access sites other than Twitter that support the Twitter
	 * API, or to set which version of the API you want to use.<br>
	 * Note: Does not include the final "/"
	 */
	String TWITTER_URL = DEFAULT_TWITTER_URL;

	private Date untilDate;

	private BigInteger untilId;

	private Long placeId;
	
	/**
	 * If set, this will place-id be sent with status-updates to geo-tag your tweets.
	 * @param placeId Can be null (which is the default)
	 * @see #setMyLocation(double[])
	 */
	public void setMyPlace(Long placeId) {
		this.placeId = placeId;
	}

	/**
	 * @deprecated ALL twitter.com endpoints now require authentication.
	 * This method is kept for use with other services (e.g. identi.ca). 
	 * <p>
	 * Create a Twitter client without specifying a user.
	 */
	public Twitter() {
		this(null, new URLConnectionHttpClient());
	}

	/**
	 * Java wrapper for the Twitter API.
	 * 
	 * @param name
	 *            the authenticating user's name, if known. Can be null.
	 * @param client
	 * @see OAuthSignpostClient
	 */
	public Twitter(String name, IHttpClient client) {
		this.name = name;
		http = client;
		assert client != null;
	}

	/**
	 * WARNING: Twitter no longer supports name/password basic authentication.
	 * This constructor is only for non-Twitter sites, such as identi.ca.
	 * 
	 * @param screenName
	 *            The name of the user. Only used by some methods.
	 * @param password
	 *            The password of the user.
	 * 
	 * @Deprecated Twitter have switched off basic authentication! Use an OAuth
	 *             client such as {@link OAuthSignpostClient} with
	 *             {@link #Twitter(String, IHttpClient)}
	 */
	@Deprecated
	public Twitter(String screenName, String password) {
		this(screenName, new URLConnectionHttpClient(screenName, password));
	}

	/**
	 * Copy constructor. Use this to pass cloned Twitter objects for
	 * multi-threaded work. This will share rate-limit info between them :)
	 * 
	 * @param jtwit
	 */
	public Twitter(Twitter jtwit) {
		this(jtwit.getScreenName(), jtwit.http.copy());
	}

	/**
	 * API methods relating to your account.
	 */
	public Twitter_Account account() {
		return new Twitter_Account(this);
	}
	
	/**
	 * API methods for Twitter stats.
	 */
	public Twitter_Analytics analytics() {
		return new Twitter_Analytics(http);
	}

	/**
	 * Add in since_id, page and count, if set. This is called by methods that
	 * return lists of statuses or messages.
	 * 
	 * @param vars
	 * @return vars
	 */
	Map<String, String> addStandardishParameters(
			Map<String, String> vars) {
		if (sinceId != null && sinceId.doubleValue() != 0) {
			String s = sinceId.toString();
			vars.put("since_id", s);
		}
		if (untilId != null) {
			vars.put("max_id", untilId.toString());
		}
		if (count != null) {
			vars.put("count", count.toString());
		}
		if (tweetEntities) {
			vars.put("include_entities", "1"); // TODO remove after testing -- this is the new default
		} else {
			vars.put("include_entities", "0");
		}
		if ( ! includeRTs) {
			vars.put("include_rts", "0"); // On is the new default
		}
		return vars;
	}

	/**
	 * Equivalent to {@link #follow(String)}. C.f.
	 * http://apiwiki.twitter.com/Migrating-to-followers-terminology
	 * 
	 * @param username
	 *            Required. The screen name of the user to befriend.
	 * @return The befriended user.
	 * @deprecated Use {@link #follow(String)} instead, which is equivalent.
	 */
	@Deprecated
	public User befriend(String username) throws TwitterException {
		return follow(username);
	}

	/**
	 * Equivalent to {@link #stopFollowing(String)}.
	 * 
	 * @deprecated Please use {@link #stopFollowing(String)} instead.
	 */
	@Deprecated
	public User breakFriendship(String username) {
		return stopFollowing(username);
	}

	/**
	 * @deprecated Use {@link Twitter_Users#show(List)} instead
	 */
	public List<User> bulkShow(List<String> screenNames) {
		return users().show(screenNames);
	}

	/**
	 * @deprecated Use {@link #showById(List)} instead
	 */
	public List<User> bulkShowById(List<? extends Number> userIds) {
		return users().showById(userIds);
	}

	/**
	 * Filter keeping only those messages that come between sinceDate and
	 * untilDate (if either or both are set). The Twitter API used to offer
	 * this, but we now have to do it client side.
	 * 
	 * @see #setSinceId(Number)
	 * 
	 * @param list
	 * @return filtered list (a copy)
	 */
	private <T extends ITweet> List<T> dateFilter(List<T> list) {
		if (sinceDate == null && untilDate == null)
			return list;
		ArrayList<T> filtered = new ArrayList<T>(list.size());
		for (T message : list) {
			// assume OK if Twitter is being stingy on the info
			if (message.getCreatedAt() == null) {
				filtered.add(message);
				continue;
			}
			if (untilDate != null && untilDate.before(message.getCreatedAt())) {
				continue;
			}
			if (sinceDate != null && sinceDate.after(message.getCreatedAt())) {
				continue;
			}
			// ok
			filtered.add(message);
		}
		return filtered;
	}

	/**
	 * Deletes the given Status or Message. The authenticating user must be the
	 * author of the status post.
	 */
	public void destroy(ITweet tweet) throws TwitterException {
		if (tweet instanceof Status) {
			destroyStatus(tweet.getId());
		} else {
			destroyMessage((Message) tweet);
		}
	}

	/**
	 * Destroy a direct message.
	 * 
	 * @param dm
	 */
	private void destroyMessage(Message dm) {
		String page = post(TWITTER_URL + "/direct_messages/destroy/" + dm.id
				+ ".json", null, true);
		assert page != null;
	}

	/**
	 * Deletes the direct message specified by the ID. The authenticating user
	 * must be the author of the specified status.
	 * 
	 * @see #destroy(ITweet)
	 */
	public void destroyMessage(Number id) {
		String page = post(TWITTER_URL + "/direct_messages/destroy/" + id
				+ ".json", null, true);
		assert page != null;
	}

	/**
	 * Deletes the status specified by the required ID parameter. The
	 * authenticating user must be the author of the specified status.
	 * 
	 * @see #destroy(ITweet)
	 */
	public void destroyStatus(Number id) throws TwitterException {
		String page = post(TWITTER_URL + "/statuses/destroy/" + id + ".json",
				null, true);				
		// Note: Sends two HTTP requests to Twitter rather than one: Twitter
		// appears
		// not to make deletions visible until the user's status page is
		// requested.
		flush();
		assert page != null;
	}

	/**
	 * Deletes the given status. Equivalent to {@link #destroyStatus(int)}. The
	 * authenticating user must be the author of the status post.
	 * 
	 * @deprecated in favour of {@link #destroy(ITweet)}. This method will be
	 *             removed by the end of 2010.
	 * @see #destroy(ITweet)
	 */
	@Deprecated
	public void destroyStatus(Status status) throws TwitterException {
		destroyStatus(status.getId());
	}

	/**
	 * Have we got enough results for the current search?
	 * 
	 * @param list
	 * @return always false if list is empty, true if maxResults is set to -1 (ie, one-page) or if list
	 *         contains maxResults or more items.
	 */
	boolean enoughResults(List list) {
		if (list.isEmpty()) return false;
		// -1 = a default of one page
		if (maxResults==-1) return true;
		return list.size() >= maxResults;
	}

	// TODO is this still needed??
	void flush() {
		// This seems to prompt twitter to update in some cases!
		http.getPage("https://twitter.com/" + name, null, true);
	}

	/**
	 * @see Twitter_Users#follow(String)
	 */
	@Deprecated
	public User follow(String username) throws TwitterException {
		return users().follow(username);
	}
	
	@Override
	public String toString() {
		return name==null? "Twitter" : "Twitter["+name+"]";
	}

	/**
	 * @see Twitter_Users#follow(User)
	 */
	@Deprecated
	public User follow(User user) {
		return follow(user.screenName);
	}

	/**
	 * Geo-location API methods.
	 * Doesn't require a logged in user.
	 */
	public Twitter_Geo geo() {
		return new Twitter_Geo(this);
	}

	/**
	 * Returns a single direct message to the authenticating user, specified by ID
	 * @param id The DM ID. 
	 */
	public Message getDirectMessage(Number id) {
		
		boolean auth = InternalUtils.authoriseIn11(this);		
		
		
		Map vars = InternalUtils.asMap("id", id);
		String json = http.getPage(TWITTER_URL + "/statuses/show/" + id
				+ ".json", vars, auth);
		try {
			Message message = new Message(new JSONObject(json));
			return message;
		} catch (JSONException e) {
			throw new TwitterException.Parsing(json, e);
		}
	}
	
	
	/**
	 * Returns a list of the direct messages sent to the authenticating user.
	 * <p>
	 * Note: the Twitter API makes this available in rss if that's of interest.
	 */
	public List<Message> getDirectMessages() {
		InternalUtils.log("jtwitter.dm", "as:"+getScreenNameIfKnown()+"...");
		return getMessages(TWITTER_URL + "/direct_messages.json",
				standardishParameters());
	}

	/**
	 * Returns a list of the direct messages sent *by* the authenticating user.
	 */
	public List<Message> getDirectMessagesSent() {
		return getMessages(TWITTER_URL + "/direct_messages/sent.json",
				standardishParameters());
	}

	/**
	 * The most recent 20 favourite tweets. (Note: This can use page - and page
	 * only - to fetch older favourites).
	 */
	public List<Status> getFavorites() {
		return getFavorites(null);
	}

	/**
	 * The most recent 20 favourite tweets for the given user.
	 * 
	 * @param screenName
	 *            login-name.
	 */
	public List<Status> getFavorites(String screenName) {
		Map<String, String> vars = InternalUtils.asMap("screen_name",
				screenName);
		return getStatuses(TWITTER_URL + "/favorites/list.json",
				addStandardishParameters(vars), http.canAuthenticate());
	}

	/**
	 * @see Twitter_Users#getFollowerIDs()
	 */
	@Deprecated
	public List<Number> getFollowerIDs() throws TwitterException {
		return users().getFollowerIDs();
	}

	/**
	 * @see Twitter_Users#getFollowerIDs(String)
	 */
	@Deprecated
	public List<Number> getFollowerIDs(String screenName)
			throws TwitterException {
		return users().getFollowerIDs(screenName);
	}

	/**
	 * @see Twitter_Users#getFollowers()
	 */
	@Deprecated
	public List<User> getFollowers() throws TwitterException {
		return users().getFollowers();
	}

	/**
	 * @see Twitter_Users#getFollowers(String)
	 */
	@Deprecated
	public List<User> getFollowers(String username) throws TwitterException {
		return users().getFollowers(username);
	}

	/**
	 * @see Twitter_Users#getFriendIDs()
	 */
	@Deprecated
	public List<Number> getFriendIDs() throws TwitterException {
		return users().getFriendIDs();
	}

	/**
	 * @see Twitter_Users#getFriendIDs(String)
	 */
	@Deprecated
	public List<Number> getFriendIDs(String screenName) throws TwitterException {
		return users().getFriendIDs(screenName);
	}

	/**
	 * @see Twitter_Users#getFriends()
	 */
	@Deprecated
	public List<User> getFriends() throws TwitterException {
		return users().getFriends();
	}

	/**
	 * @see Twitter_Users#getFriendss(String)
	 */
	@Deprecated
	public List<User> getFriends(String username) throws TwitterException {
		return users().getFriends(username);
	}

	/**
	 * Returns the 20 most recent statuses posted in the last 24 hours from the
	 * authenticating user and that user's friends.
	 * 
	 * @deprecated Replaced by {@link #getHomeTimeline()}
	 */
	@Deprecated
	public List<Status> getFriendsTimeline() throws TwitterException {
		return getHomeTimeline();
	}

	/**
	 * Returns the 20 most recent statuses posted in the last 24 hours from the
	 * authenticating user and that user's friends, including retweets.
	 */
	public List<Status> getHomeTimeline() throws TwitterException {
		assert http.canAuthenticate();
		return getStatuses(TWITTER_URL + "/statuses/home_timeline.json",
				standardishParameters(), true);
	}

	/**
	 * Provides access to the {@link IHttpClient} which manages the low-level
	 * authentication, posts and gets.
	 */
	public IHttpClient getHttpClient() {
		return http;
	}

	/**
	 * @return your lists, ie. the one's you made.
	 */
	public List<TwitterList> getLists() {
		return getLists(name);
	}
	
	/**
	 * 
		Returns <i>all</i> lists the authenticating or specified user subscribes to, 
		including their own.
	   @param user can be null for the authenticating user.
	   @see #getLists(String)
	 */
	public List<TwitterList> getListsAll(User user) {		
		assert user!=null || http.canAuthenticate() : "No authenticating user";
		try {
			String url = TWITTER_URL + "/lists/all.json";
			Map<String, String> vars = user.screenName==null?
					InternalUtils.asMap("user_id", user.id)
					: InternalUtils.asMap("screen_name", user.screenName);
			String listsJson = http.getPage(url, vars, http.canAuthenticate());
			JSONObject wrapper = new JSONObject(listsJson);
			JSONArray jarr = (JSONArray) wrapper.get("lists");
			List<TwitterList> lists = new ArrayList<TwitterList>();
			for (int i = 0; i < jarr.length(); i++) {
				JSONObject li = jarr.getJSONObject(i);
				TwitterList twList = new TwitterList(li, this);
				lists.add(twList);
			}
			return lists;
		} catch (JSONException e) {
			throw new TwitterException.Parsing(null, e);
		}
	}

	/**
	 * @param screenName
	 * @return the (first 20) lists created by the given user
	 */
	public List<TwitterList> getLists(String screenName) {
		assert screenName != null;
		try {
			String url = TWITTER_URL + "/lists/list.json";
			Map<String, String> vars = InternalUtils.asMap(
					"screen_name", screenName);
			String listsJson = http.getPage(url, vars, true);
//			JSONObject wrapper = new JSONObject(listsJson);
			JSONArray jarr = new JSONArray(listsJson); // wrapper.get("lists");
			List<TwitterList> lists = new ArrayList<TwitterList>();
			for (int i = 0; i < jarr.length(); i++) {
				JSONObject li = jarr.getJSONObject(i);
				TwitterList twList = new TwitterList(li, this);
				lists.add(twList);
			}
			return lists;
		} catch (JSONException e) {
			throw new TwitterException.Parsing(null, e);
		}
	}

	/**
	 * @param screenName
	 * @param filterToOwned
	 *            If true, only return lists which the user owns.
	 * @return lists of which screenName is a member. NOTE: currently limited to
	 *         a maximum of 20 lists!
	 */
	public List<TwitterList> getListsContaining(String screenName,
			boolean filterToOwned) {
		assert screenName != null;
		try {
			String url = TWITTER_URL + "/lists/memberships.json";
			Map<String, String> vars = InternalUtils.asMap("screen_name",
					screenName);
			if (filterToOwned) {
				assert http.canAuthenticate();
				vars.put("filter_to_owned_lists", "1");
			}
			String listsJson = http.getPage(url, vars, http.canAuthenticate());
			JSONObject wrapper = new JSONObject(listsJson);
			JSONArray jarr = (JSONArray) wrapper.get("lists");
			List<TwitterList> lists = new ArrayList<TwitterList>();
			for (int i = 0; i < jarr.length(); i++) {
				JSONObject li = jarr.getJSONObject(i);
				TwitterList twList = new TwitterList(li, this);
				lists.add(twList);
			}
			return lists;
		} catch (JSONException e) {
			throw new TwitterException.Parsing(null, e);
		}
	}

	/**
	 * Convenience for {@link #getListsContaining(String, boolean)}.
	 * 
	 * @return lists that you are a member of. Warning: currently limited to a
	 *         maximum of 20 results.
	 */
	public List<TwitterList> getListsContainingMe() {
		return getListsContaining(name, false);
	}

	/**
	 * @deprecated Use {@link TwitLonger}
	 * @param truncatedStatus
	 *            If this is a twitlonger.com truncated status, then call
	 *            twitlonger to fetch the full text.
	 * @return the full status message. If this is not a twitlonger status, this
	 *         will just return the status text as-is.
	 * @see #updateLongStatus(String, long)
	 */
	public String getLongStatus(Status truncatedStatus) {
		TwitLonger tl = new TwitLonger();
		return tl.getLongStatus(truncatedStatus);
	}

	/**
	 * Provides support for fetching many pages. -1 indicates "give me 1 page's worth"
	 */
	public int getMaxResults() {
		return maxResults;
	}

	/**
	 * Returns the 20 most recent replies/mentions (status updates with
	 * \@username) to the authenticating user. Replies are only available to the
	 * authenticating user; you cannot request a list of replies to another
	 * user whether public or protected.
	 * <p>
	 * This is exactly the same as {@link #getReplies()}
	 * <p>
	 * When paging, this method can only go back up to 800 statuses.
	 * <p>
	 * Does not include new-style retweets.
	 */
	public List<Status> getMentions() {
		return getStatuses(TWITTER_URL + "/statuses/mentions_timeline.json",
				standardishParameters(), true);
	}

	/**
	 * 
	 * @param url
	 * @param var
	 * @param isPublic
	 *            Value to set for Message.isPublic
	 * @return
	 */
	private List<Message> getMessages(String url, Map<String, String> var) {
		// Twitter truncates DMs to 140 chars to maintain compatibility with older apps
		// Add param "full_text=true" to query to get full text
		if(!var.containsKey("full_text")) {
			var.put("full_text", "true");
		}
		// Default: 1 page
		if (maxResults < 1) {			
			String p = http.getPage(url, var, true);
			// DEBUG Investigating slow delivery to coopbankuk_help TODO delete
			if (url.contains("direct_messages")) {
				InternalUtils.log("jtwitter.dm", "as:"+getScreenNameIfKnown()+" "+url+" "+var);
			}
			List<Message> msgs = Message.getMessages(p);
			msgs = dateFilter(msgs);
			return msgs;
		}
		// Fetch all pages until we run out
		// -- or Twitter complains in which case you'll get an exception
		BigInteger maxId = untilId;
		List<Message> msgs = new ArrayList<Message>();
		while (msgs.size() <= maxResults) {
			// DEBUG Investigating slow delivery to coopbankuk_help TODO delete
			if (url.contains("direct_messages")) {
				InternalUtils.log("jtwitter.dm", "as:"+getScreenNameIfKnown()+" "+url+" "+var+" already-got:"+msgs.size());
			}
			String p = http.getPage(url, var, true);
			List<Message> nextpage = Message.getMessages(p);
			// Next page must start strictly before this one
			maxId = InternalUtils.getMinId(maxId, nextpage);
			List<Message> nextpageDateFiltered = dateFilter(nextpage);
			msgs.addAll(nextpageDateFiltered); 
			if (nextpage.size() < 20) {
				// No more results to get
				break;
			}	
			// TODO Can we stop early if the date-filter kicked in?
			// Twitter goes most-recent-first, so we could stop now if the sinceDate was actively filtering. 
			var.put("max_id", maxId.toString());
		}
		return msgs;
	}

	/**
	 * @deprecated Use getHttpClient().getRateLimits()
	 * What is the current rate limit status? Do we need to throttle back our
	 * usage? This is the cached info from the last call of that type.
	 * <p>
	 * Note: The RateLimit object is created using cached info from a previous
	 * Twitter call. So this method is quick (it doesn't require a fresh call to
	 * Twitter), but the RateLimit object isn't available until after you make a
	 * call of the right type to Twitter.
	 * <p>
	 * Status: Heading towards stable, but still a bit experimental.
	 * 
	 * @param reqType
	 *            Different methods have separate rate limits.
	 * @return the last rate limit advice received, or null if unknown.
	 * @see #getRateLimitStatus()
	 */
	public RateLimit getRateLimit(KRequestType reqType) {
		return http.getRateLimit(reqType);
	}
	
	/**
	 * @deprecated Not in v1.1
	 * 
	 * How many normal rate limit calls do you have left? This calls Twitter,
	 * which makes it slower than {@link #getRateLimit(KRequestType)} but it's
	 * up-to-date and safe against threads and other-programs using the same
	 * allowance.
	 * <p>
	 * This may update getRateLimit(KRequestType) for NORMAL requests, but sadly
	 * it doesn't fetch rate-limit info on other request types.
	 * 
	 * @return the remaining number of API requests available to the
	 *         authenticating user before the API limit is reached for the
	 *         current hour. <i>If this is zero or negative you should stop
	 *         using Twitter with this login for a bit.</i> Note: Calls to
	 *         rate_limit_status do not count against the rate limit.
	 * @see #getRateLimit(KRequestType)
	 */
	public int getRateLimitStatus() {
		RateLimit rl = ((URLConnectionHttpClient)http).updateRateLimits().get(KRequestType.NORMAL.rateLimit);
		return rl==null? 90 : rl.getRemaining();
	}
	
	
	/**
	 * Returns the 20 most recent replies/mentions (status updates with
	 * 
	 * @username) to the authenticating user. Replies are only available to the
	 *            authenticating user; you can not request a list of replies to
	 *            another user whether public or protected.
	 *            <p>
	 *            This is exactly the same as {@link #getMentions()}! Twitter
	 *            changed their API & terminology - we are (currently) keeping
	 *            both methods.
	 *            <p>
	 *            When paging, this method can only go back up to 800 statuses.
	 *            <p>
	 *            Does not include new-style retweets.
	 * @deprecated Use #getMentions() for preference. This method will be removed June 2013.
	 */
	public List<Status> getReplies() throws TwitterException {
		return getMentions();
	}

	/**
	 * Show users who (new-style) retweeted the given tweet. Can use count (up
	 * to 100) and page. This does not include old-style retweeters!
	 * 
	 * @param tweet
	 *            You can use a "fake" Status created via
	 *            {@link Status#Status(User, String, long, Date)} if you know
	 *            the id number.
	 */
	public List<User> getRetweeters(Status tweet) {
		String url = TWITTER_URL + "/statuses/retweets/" + tweet.id
				+ ".json";
		Map<String, String> vars = addStandardishParameters(new HashMap<String, String>());
		String json = http.getPage(url, vars, http.canAuthenticate());
		List<Status> ss = Status.getStatuses(json);		
		List<User> users = new ArrayList(ss.size());
		for (Status status : ss) {
			users.add(status.getUser());
		}
		return users;
	}

	/**
	 * @return Retweets of this tweet. This attempts to cover new-style and
	 *         old-style "manual" retweets. It does so by making retweet call
	 *         and a search call. It will miss edited retweets though.
	 */
	public List<Status> getRetweets(Status tweet) {
		String url = TWITTER_URL + "/statuses/retweets/" + tweet.id + ".json";
		Map<String, String> vars = addStandardishParameters(new HashMap<String, String>());
		String json = http.getPage(url, vars, true);
		List<Status> newStyle = Status.getStatuses(json);
		try {
			// // Should we also do by search and merge the two lists?
			StringBuilder sq = new StringBuilder();
			sq.append("\"RT @" + tweet.getUser().getScreenName() + ": ");
			if (sq.length() + tweet.text.length() + 1 > 140) {
				int i = tweet.text.lastIndexOf(' ', 140 - sq.length() - 1);
				String words = tweet.text.substring(0, i);
				sq.append(words);
			} else {
				sq.append(tweet.text);
			}
			sq.append('"');
			List<Status> oldStyle = search(sq.toString());
			// merge them
			newStyle.addAll(oldStyle);
			Collections.sort(newStyle, InternalUtils.NEWEST_FIRST);
			return newStyle;
		} catch (TwitterException e) {
			// oh well
			return newStyle;
		}
	}

	/**
	 * @deprecated Removed in api v1.1. Simulated with other methods. Will be removed June 2013
	 * 
	 * @return retweets that you have made using "new-style" retweets rather
	 *         than the RT microfromat. These are your tweets, i.e. they begin
	 *         "RT @whoever: ". You can get the original tweet via
	 *         {@link Status#getOriginal()}
	 */
	public List<Status> getRetweetsByMe() {
		List<Status> myTweets = getUserTimeline();
		List<Status> retweets =new ArrayList();
		for (Status status : myTweets) {
			if (status.getOriginal()!=null && status.getText().startsWith("RT")) {
				retweets.add(status);
			}
		}
		return retweets;
	}

	/**
	 * @return those of your tweets that have been retweeted. It's a bit of a
	 *         strange one this. You can then query who retweeted you.
	 */
	public List<Status> getRetweetsOfMe() {
		String url = TWITTER_URL + "/statuses/retweets_of_me.json";
		Map<String, String> vars = addStandardishParameters(new HashMap<String, String>());
		String json = http.getPage(url, vars, true);
		return Status.getStatuses(json);
	}

	/**
	 * @return Login name of the authenticating user, or null if not set.
	 *         <p>
	 *         Will call Twitter to find out if null but oauth is set.
	 * @see #getSelf()
	 */
	public String getScreenName() {
		if (name != null)
			return name;
		// load if need be
		getSelf();
		return name;
	}
	
	/**
	 * Equivalent to {@link #getScreenName()} except this won't ever do
	 * an API call.
	 * @return screenName or null
	 * @see #getScreenName()
	 */
	public String getScreenNameIfKnown() {
		return name;
	}

	/**
	 * @param searchTerm
	 * @param rpp
	 * @return
	 */
	private Map<String, String> getSearchParams(String searchTerm, Integer rpp) {
		Map vars = InternalUtils.asMap(
				"count", rpp, 
				"q", searchTerm);
		if (sinceId != null && sinceId.doubleValue()!=0) {
			vars.put("since_id", sinceId.toString());
		}
		if (untilId != null) {
			// It's unclear from the docs whether this will work
			// c.f. https://dev.twitter.com/docs/api/1/get/search
			vars.put("max_id", untilId.toString());
		}
		// since date is no longer supported. until is though?!
		// if (sinceDate != null) vars.put("since", df.format(sinceDate));
		if (untilDate != null) {
			vars.put("until", InternalUtils.df.format(untilDate));
		}
		if (lang != null) {
			vars.put("lang", lang);
		}
		if (geocode != null) {
			vars.put("geocode", geocode);
		}
		if (resultType != null) {
			vars.put("result_type", resultType);
		}
		addStandardishParameters(vars);
		return vars;
	}

	/**
	 * @return you, or null if this is an anonymous Twitter object.
	 *         <p>
	 *         This will cache the result if it makes an API call.
	 */
	public User getSelf() {
		if (self != null)
			return self;
		if (!http.canAuthenticate()) {
			if (name != null) {
				// not sure this case makes sense, but we may as well handle it
				self = new User(name);
				return self;
			}
			return null;
		}
		account().verifyCredentials();
		name = self.getScreenName();
		return self;
	}

	/**
	 * @return The current status of the user. Warning: this is null if (a)
	 *         unset (ie if this user has never tweeted), or (b) their last six
	 *         tweets were all new-style retweets!
	 * @see #getUserTimeline()
	 * <p>
	 * Minor Warning: There can be a very slight delay in Twitter for a status-update 
	 * to take effect (i.e. for the tweet to become visible). Which means if you have
	 * *just* called updateStatus(), then getStatus() may not match.  
	 */
	public Status getStatus() throws TwitterException {
		Map<String, String> vars = InternalUtils.asMap("count", 6);
		String json = http.getPage(
				TWITTER_URL + "/statuses/user_timeline.json", vars, true);
		List<Status> statuses = Status.getStatuses(json);
		if (statuses.size() == 0)
			return null;
		return statuses.get(0);
	}

	/**
	 * Returns a single status, specified by the id parameter below. The
	 * status's author will be returned inline.
	 * 
	 * @param id
	 *            The numerical ID of the status you're trying to retrieve.
	 */
	public Status getStatus(Number id) throws TwitterException {
		boolean auth = InternalUtils.authoriseIn11(this);
		Map vars = tweetEntities ? InternalUtils.asMap("include_entities", "1")
				: null;
		String json = http.getPage(TWITTER_URL + "/statuses/show/" + id
				+ ".json", vars, auth);
		try {
			return new Status(new JSONObject(json), null);
		} catch (JSONException e) {
			throw new TwitterException.Parsing(json, e);
		}
	}

	/**
	 * @return The current status of the given user.
	 *         <p>
	 *         Warning: this can be null if the user has been doing enough
	 *         new-style retweets. This is due to flaws in the Twitter API.
	 */
	public Status getStatus(String username) throws TwitterException {
		assert username != null;
		// new-style retweets can cause blanks in your timeline
		// show(username).status is just as vulnerable
		// grab a few tweets to give some robustness
		Map<String, String> vars = InternalUtils.asMap("id", username, "count",
				6);
		String json = http.getPage(
				TWITTER_URL + "/statuses/user_timeline.json", vars, http.canAuthenticate());
		List<Status> statuses = Status.getStatuses(json);
		if (statuses.size() == 0)
			return null;
		return statuses.get(0);
	}

	/**
	 * Does the grunt work for paged status fetching
	 * 
	 * @param url
	 * @param var
	 * @param authenticate
	 * @return
	 */
	List<Status> getStatuses(final String url, Map<String, String> var,
			boolean authenticate) 
	{
		// Default: 1 page
		if (maxResults < 1) {
			List<Status> msgs ;
			try {
				msgs = Status.getStatuses(http.getPage(url, var,
						authenticate));
			} catch (TwitterException.Parsing pex) {
				// Twitter bug, July 2012: malformed responses -- end is chopped off ~1 time in 20
				// TODO remove when Twitter fix this!
				if (http.isRetryOnError()) {
					InternalUtils.sleep(250);
					String json = http.getPage(url, var, authenticate);
					msgs = Status.getStatuses(json);
				} else {
					throw pex;
				}
			}
			msgs = dateFilter(msgs);
			return msgs;
		}
		// Fetch all pages until we reach the desired maxResults, or run out
		// -- or Twitter complains in which case you'll get an exception
		// Use status ids for paging, rather than page number, because this
		// allows for "drift" when new tweets are posted during the paging.
		BigInteger maxId = untilId;
		List<Status> msgs = new ArrayList<Status>();

		while (msgs.size() <= maxResults) {			
			List<Status> nextpage; 
			try {
				String json = http.getPage(url, var, authenticate);
				nextpage = Status.getStatuses(json);
			} catch (TwitterException.Parsing pex) {
				// Twitter bug, July 2012: malformed responses -- end is chopped off ~1 time in 20
				// TODO remove when Twitter fix this!
				if (http.isRetryOnError()) {
					InternalUtils.sleep(250);
					String json = http.getPage(url, var, authenticate);
					nextpage = Status.getStatuses(json);
				} else {
					throw pex;
				}
			}
			// This test replaces size<20. It requires an extra call to Twitter.
			// But it fixes a bug whereby retweets aren't counted and can thus
			// cause the system to quit early.
			if (nextpage.size() == 0) {
				break;
			}	
			
			// Next page must start strictly before this one
			maxId = InternalUtils.getMinId(maxId, nextpage);
			Date maxDate = InternalUtils.getMaxDate(nextpage);
			List<Status> filtered = dateFilter(nextpage);
			msgs.addAll(filtered);
			// If we've passed the sinceDate, and we've started to filter messages, to the point
			// where we receive none. then we need to stop probing to avoid looping over ever-older messages,
			// and the rate-limiting that'll cause.
			if (filtered.size() == 0 && sinceDate != null && sinceDate.after(maxDate)){
				break;
			}
			var.put("max_id", maxId.toString());			
		}
		return msgs;
	}
	
	public InputStream getDMImage(Message msg) {
		List<TweetEntity> entities = msg.getTweetEntities(KEntityType.media);
		if (entities.isEmpty()) return null;
		String mediaUrl = entities.get(0).mediaUrl();
		try {
			HttpURLConnection connection = http.connect(mediaUrl, null, true);
			return connection.getInputStream();
			
		} catch (IOException e) {
			return null;
		}
	}

	/**
	 * @return the latest global trending topics on Twitter
	 */
	public List<String> getTrends() {
		return getTrends(1);
	}

	/**
	 * @param a
	 *            Yahoo Where-on-Earth ID. c.f.
	 *            http://developer.yahoo.com/geo/geoplanet/
	 * @return the latest regional trending topics on Twitter
	 * @see Twitter_Geo#getTrendRegions()
	 */
	public List<String> getTrends(Number woeid) {
		String jsonTrends = http.getPage(TWITTER_URL + "/trends/place.json", 
				InternalUtils.asMap("id", woeid), true);
		try {
			JSONArray jarr = new JSONArray(jsonTrends);
			JSONObject json1 = jarr.getJSONObject(0);
			JSONArray json2 = json1.getJSONArray("trends");
			List<String> trends = new ArrayList<String>();
			for (int i = 0; i < json2.length(); i++) {
				JSONObject ti = json2.getJSONObject(i);
				String t = ti.getString("name");
				trends.add(t);
			}
			return trends;
		} catch (JSONException e) {
			throw new TwitterException.Parsing(jsonTrends, e);
		}
	}

	/**
	 * @return the untilDate
	 */
	public Date getUntilDate() {
		return untilDate;
	}

	/**
	 * @see Twitter_Users#getUser(long)
	 */
	@Deprecated
	public User getUser(long userId) {
		return show(userId);
	}

	/**
	 * @see Twitter_Users#getUser(String)
	 */
	@Deprecated
	public User getUser(String screenName) {
		return show(screenName);
	}

	/**
	 * Returns the most recent statuses from the authenticating user. 20 by
	 * default.
	 */
	public List<Status> getUserTimeline() throws TwitterException {
		return getStatuses(TWITTER_URL + "/statuses/user_timeline.json",
				standardishParameters(), true);
	}

	/**
	 * Equivalent to {@link #getUserTimeline(String)}, but takes a numeric
	 * user-id instead of a screen-name.
	 * 
	 * @param userId
	 * @return tweets by userId
	 */
	public List<Status> getUserTimeline(Long userId) throws TwitterException {
		Map<String, String> vars = InternalUtils.asMap("user_id", userId);
		addStandardishParameters(vars);
		// Authenticate if we can (for protected streams)
		boolean authenticate = http.canAuthenticate();
		try {
			return getStatuses(TWITTER_URL + "/statuses/user_timeline.json",
					vars, authenticate);
		} catch (E401 e) {
			// Bug in Twitter: this can be a suspended user...
			// In which case the call below would generate a SuspendedUser exception
			// ...but do we want to conserve our api limit??
			// isSuspended(userId);
			throw e;
		}
	}

	/**
	 * Returns the most recent statuses from the given user.
	 * <p>
	 * This will return 20 results by default, though
	 * {@link #setMaxResults(int)} can be used to fetch multiple pages.
	 * 
	 * Note that if you exclude new-style retweets (via
	 * {@link #setIncludeRTs(boolean)}) then this can return less than 20
	 * results -- it can even return none if the latest 20 are all retweets.
	 * <p>
	 * There is a cap of 3200 tweets - this is the farthest back you can go down
	 * a user timeline!
	 * <p>
	 * This method will authenticate if it can (i.e. if the Twitter object has a
	 * username and password). Authentication is needed to see the posts of a
	 * private user.
	 * 
	 * @param screenName
	 *            Can be null. Specifies the screen name of the user for whom to
	 *            return the user_timeline.
	 * @throws TwitterException.E401
	 *             if the user has protected their tweets, and you do not have
	 *             access.
	 * @throws TwitterException.SuspendedUser
	 *             if the user has been suspended
	 */
	public List<Status> getUserTimeline(String screenName)
			throws TwitterException {
		Map<String, String> vars = InternalUtils.asMap("screen_name",
				screenName);
		addStandardishParameters(vars);
		// Should we authenticate?
		boolean authenticate = http.canAuthenticate();
		try {
			return getStatuses(TWITTER_URL + "/statuses/user_timeline.json",
					vars, authenticate);
		} catch (E404 e){
			throw new TwitterException.E404("Twitter does not return any information for " + screenName + 
					". They may have been deleted long ago.");
		} catch (E401 e) {
			// Bug in Twitter: this can be a suspended user 
			// - in which case this will generate a SuspendedUser exception
			isSuspended(screenName);
			throw e;
		} 
	}

	/**
	 * @deprecated Use {@link #setIncludeRTs(boolean)} instead to control
	 *             retweet behaviour.
	 * 
	 *             Returns the most recent statuses posted by the given user.
	 *             Unlike {@link #getUserTimeline(String)}, this includes
	 *             new-style retweets.
	 *             <p>
	 *             This will return 20 by default, though
	 *             {@link #setMaxResults(int)} can be used to fetch multiple
	 *             pages. There is a cap of 3200 tweets - this is the farthest
	 *             back you can go down a user timeline!
	 *             <p>
	 *             This method will authenticate if it can (i.e. if the Twitter
	 *             object has a username and password). Authentication is needed
	 *             to see the posts of a private user.
	 * 
	 @param screenName
	 *            Can be null. Specifies the screen name of the user for whom to
	 *            return the user_timeline.
	 * 
	 */
	public List<Status> getUserTimelineWithRetweets(String screenName)
			throws TwitterException {
		Map<String, String> vars = InternalUtils.asMap("screen_name",
				screenName, "include_rts", "1");
		addStandardishParameters(vars);
		// Should we authenticate?
		boolean authenticate = http.canAuthenticate();
		try {
			return getStatuses(TWITTER_URL + "/statuses/user_timeline.json",
					vars, authenticate);
		} catch (E401 e) {
			isSuspended(screenName);
			throw e;
		}
	}

	/**
	 * @see Twitter_Users#isFollower(String)
	 */
	@Deprecated
	public boolean isFollower(String userB) {
		return isFollower(userB, name);
	}

	/**
	 * @deprecated 
	 * @see Twitter_Users#isFollower(String, String)
	 */
	public boolean isFollower(String followerScreenName,
			String followedScreenName) {
		return users().isFollower(followerScreenName, followedScreenName);
	}

	/**
	 * @see Twitter_Users#isFollowing(String)
	 */
	@Deprecated
	public boolean isFollowing(String userB) {
		return isFollower(name, userB);
	}

	/**
	 * @see Twitter_Users#isFollowing(User)
	 */
	@Deprecated
	public boolean isFollowing(User user) {
		return isFollowing(user.screenName);
	}

	/**
	 * @deprecated
	 * Are we rate-limited, based on cached info from previous requests?
	 * @param type
	 * @param minCalls
	 *            Standard value = 1. The minimum number of calls which should
	 *            be available.
	 * @return true if this is currently rate-limited, & should not be used for
	 *         a while. false = OK
	 *         
	 * @see #getRateLimit(KRequestType) for more info
	 * @see #getRateLimitStatus() for guaranteed up-to-date info
	 */
	public boolean isRateLimited(KRequestType reqType, int minCalls) {
		RateLimit rl = getRateLimit(reqType);		
		// assume things are OK
		if (rl == null) {
			return false;
		}
		// in credit?
		if (rl.getRemaining() >= minCalls)
			return false;
		// out of date?
		if (rl.isOutOfDate())
			return false;
		// nope - you're over the limit
		return true;
	}

	/**
	 * Generate an exception if the use is suspended. This is used as a
	 * work-around for misleading error codes returned by Twitter.
	 * 
	 * @param screenName
	 * @throws SuspendedUser
	 */
	private void isSuspended(String screenName) throws SuspendedUser {
		show(screenName);
	}

	/**
	 * @deprecated Use {@link TwitLonger}
	 * Keeping for backwards compatibility until Q2 2013
	 * 
	 * @return true if {@link #setupTwitlonger(String, String)} has been used to
	 *         provide twitlonger.com details.
	 * @see #updateLongStatus(String, long)
	 */	
	public boolean isTwitlongerSetup() {
		return twitlongerApiKey != null && twitlongerAppName != null;
	}

	/**
	 * Are the login details used for authentication valid?
	 * 
	 * @return true if OK, false if unset or invalid
	 * @see Twitter_Account#verifyCredentials() which returns user info
	 */
	public boolean isValidLogin() {
		if (!http.canAuthenticate())
			return false;
		try {
			Twitter_Account ta = new Twitter_Account(this);
			User u = ta.verifyCredentials();
			return true;
		} catch (TwitterException.E403 e) {
			return false;
		} catch (TwitterException.E401 e) {
			return false;
		} catch (TwitterException e) {
			throw e;
		}
	}

	/**
	 * Wrapper for {@link IHttpClient#post(String, Map, boolean)}.
	 */
	private String post(String uri, Map<String, String> vars,
			boolean authenticate) throws TwitterException {
		String page = http.post(uri, vars, authenticate);
		return page;
	}

	/**
	 * Report a user for being a spammer.
	 * 
	 * @param screenName
	 */
	public void reportSpam(String screenName) {
		http.getPage(TWITTER_URL + "/version/report_spam.json",
				InternalUtils.asMap("screen_name", screenName), true);
	}

	/**
	 * Retweet (new-style) a tweet without any edits. You can also retweet by
	 * starting a status using the RT @username microformat. (this is an
	 * old-style retweet).
	 * 
	 * @param tweet
	 *            Note: you cannot retweet your own tweets.
	 * @return your retweet
	 */
	public Status retweet(Status tweet) {
		try {
			String result = post(
					TWITTER_URL + "/statuses/retweet/" + tweet.getId()
							+ ".json", null, true);
			return new Status(new JSONObject(result), null);

			// error handling
		} catch (E403 e) {
			List<Status> rts = getRetweetsByMe();
			for (Status rt : rts) {
				if (tweet.equals(rt.getOriginal()))
					throw new TwitterException.Repetition(rt.getText());
			}
			throw e;
		} catch (JSONException e) {
			throw new TwitterException.Parsing(null, e);
		}
	}
	

	/**
	 * Retweet, adding a comment. This is also known as a Quote Tweet.
	 * See https://support.twitter.com/articles/20169873
	 * @param tweet
	 * @param comment This must be 116 characters or less, as the retweet counts like a url (which, technically, is what it is here).
	 * @return the retweet
	 */
	public Status retweetWithComment(Status tweet, String comment) {
		if (comment==null) return retweet(tweet);
		comment = comment.trim();
		if (comment.length()==0) return retweet(tweet);
		// should we mark it in reply to tweet??
		Status s = setStatus(comment+" "+tweet.getUrl());
		return s;
	}

	/**
	 * Perform a search of Twitter. Convenience wrapper for
	 * {@link #search(String, ICallback, int)} with no callback and fetching one
	 * pages worth of results.
	 */
	public List<Status> search(String searchTerm) {
		return search(searchTerm, null, 100);
	}

	/**
	 * Perform a search of Twitter.
	 * <p>
	 * Warning: the User objects returned by a search (as part of the Status
	 * objects) are dummy-users. The only information that is set is the user's
	 * screen-name and a profile image url. This reflects the current behaviour
	 * of the Twitter API. If you need more info, call users().show()
	 * with the screen names.
	 * <p>
	 * This supports {@link #maxResults} and pagination. A language filter can
	 * be set via {@link #setLanguage(String)} Location can be set via
	 * {@link #setSearchLocation(double, double, String)}
	 * 
	 * Other advanced search features can be done via the query string. E.g.<br>
	 * "from:winterstein" - tweets from user winterstein<br>
	 * "to:winterstein" - tweets start with @winterstein<br>
	 * "source:jtwitter" - originating from the application JTwitter - your
	 * query must also must contain at least one keyword parameter. <br>
	 * "filter:links" - tweets contain a link<br>
	 * "apples OR pears" - or ("apples pears" would give you apples <i>and</i>
	 * pears).
	 * 
	 * @param searchTerm
	 *            This can include several space-separated keywords, #tags and @username
	 *            (for mentions), and use quotes for \"exact phrase\" searches.
	 *        Limited to 1,000 characters maximum, including operators. 
	 *        Queries may additionally be limited by complexity.
	 * @param callback
	 *            an object whose process() method will be called on each new
	 *            page of results.
	 * @param rpp
	 *            results per page. 100 is the default
	 * @return search results - up to maxResults if maxResults is positive, or
	 *         rpp if maxResults is negative/zero. See
	 *         {@link #setMaxResults(int)} to use > 100.
	 *         
	 * @throws E403 or E406 if the search query can't be handled.
	 */
	public List<Status> search(String searchTerm, ICallback callback, int rpp) {
		// TODO refactor to use the metadata returned from Twitter
		if (rpp > 100 && maxResults < rpp)
			throw new IllegalArgumentException(
					"You need to switch on paging to fetch more than 100 search results. First call setMaxResults() to raise the limit above "
							+ rpp);
		// Too long a query?
		if (searchTerm.length() > 1000) {
			throw new TwitterException.E406("Search query too long: "+searchTerm);
			// Note: queries can still be rejected by twitter on complexity grounds.  
		}
//		searchTerm = search2_bugHack(searchTerm);
		Map vars;
		if (maxResults < 100 && maxResults > 0) {
			// Default: 1 page
			vars = getSearchParams(searchTerm, maxResults);
		} else {
			vars = getSearchParams(searchTerm, rpp);
		}
		// Fetch all pages until we run out
		// -- or Twitter complains in which case you'll get an exception
		List<Status> allResults = new ArrayList<Status>(Math.max(maxResults,
				rpp));
		String url = TWITTER_URL + "/search/tweets.json";
		BigInteger maxId = untilId;
		do {
			vars.put("max_id", maxId);
			List<Status> stati;
			try {
				String json = http.getPage(url, vars, true);
				stati = Status.getStatusesFromSearch(this, json);
			} catch (TwitterException.Parsing pex) {
				// Twitter bug, July 2012: malformed responses -- end is chopped off ~1 time in 20
				// TODO remove when Twitter fix this!
				if (http.isRetryOnError()) {
					InternalUtils.sleep(250);
					String json = http.getPage(url, vars, true);
					stati = Status.getStatusesFromSearch(this, json);
				} else {
					throw pex;
				}
			} catch(TwitterException.E403 ex) {
				// Try to send a more helpful error message TODO keep an eye out that this remains valid
				if (ex.getMessage()!=null && ex.getMessage().startsWith("code 195:")) {
					throw new TwitterException.E406("Search too long/complex: "+ex.getMessage());
				}
				throw ex;
			}
			int numResults = stati.size();
			
			maxId = InternalUtils.getMinId(maxId, stati);
			
			stati = dateFilter(stati);
			allResults.addAll(stati);
			if (callback != null) {
				// the callback may tell us to stop, by returning true
				if (callback.process(stati)) {
					break;
				}
			}
			if ((rpp==100 && numResults<70/* allow for some screening */) || numResults < rpp) { // We've reached the end of the results
				break;
			}
		} while (allResults.size() < maxResults);
		return allResults;
	}

	/* DISABLED, but kept in code, just in case.
	 * 
	 * This fixes a couple of bugs in Twitter's search API:
	 * 
	 * 1. Searches using OR and a location return gibberish, unless they also
	 * include a -term. Strangely that seems to fix things. So we just add one
	 * if needed.<br>
	 * 
	 * 2. Searches that start and end with quotes, and use an OR have problems:
	 * they become AND searches with the OR turned into a keyword. E.g. /"apple"
	 * OR "pear"/ acts like /"apple" AND or AND "pear"/
	 * <p>
	 * It should be tested periodically whether we need this. See
	 * {@link TwitterTest#testSearchBug()}, {@link TwitterTest#testSearchBug2()}
	 * 
	 * @param searchTerm
	 * @return e.g. "apples OR pears" goes to
	 *         "apples OR pears -kfz"
	 
	private String search2_bugHack(String searchTerm) {
//		if (true) return searchTerm; TODO Looks like this is no longer needed (quick test, 4th Nov 2012)
		// zero-length is valid with location
		if (searchTerm.length()==0)
			return searchTerm;
		// bug 1: a OR b near X fails
		if (searchTerm.contains(" OR ") && !searchTerm.contains("-")
				&& geocode != null)
			return searchTerm + " -kfz"; // add a -gibberish term
		// bug 2: "a" OR "b" fails
		if (searchTerm.contains(" OR ") && searchTerm.charAt(0) == '"'
				&& searchTerm.charAt(searchTerm.length() - 1) == '"')
			return searchTerm + " -kfz"; // add a -gibberish term
		// hopefully fine as-is
		return searchTerm;
	}
*/
	/**
	 * @see Twitter_Users#searchUsers(String)
	 */
	@Deprecated
	public List<User> searchUsers(String searchTerm) {
		return users().searchUsers(searchTerm);
	}

	/**
	 * Sends a new direct message (DM) to the specified user from the
	 * authenticating user. This is a private message!
	 * 
	 * @param recipient
	 *            Required. The screen name of the recipient user. This does *not* start with an "@".
	 * @param text
	 *            Required. The text of your direct message. Keep it under 140
	 *            characters! This should *not* include the "d username" portion
	 * @return the sent message
	 * @throws TwitterException.E403
	 *             if the recipient is not following you. (you can \@mention
	 *             anyone but you can only dm people who follow you).
	 */
	public Message sendMessage(String recipient, String text) throws TwitterException {		
		assert recipient != null && text != null : recipient + " " + text;
		assert ! text.startsWith("d " + recipient) : recipient + " " + text;
		assert ! recipient.startsWith("@") : recipient + " " + text;
		if (text.length() > MAX_DM_LENGTH)
			throw new IllegalArgumentException("Message is too long.");
		Map<String, String> vars = InternalUtils.asMap(
				"screen_name", recipient,
				"text", text);
		if (tweetEntities) {
			vars.put("include_entities", "1");
		}
		String result = null;
		try {
			// post it
			result = post(TWITTER_URL + "/direct_messages/new.json", vars, true);
			// sadly the response doesn't include rate-limit info
			return new Message(new JSONObject(result));
		} catch (JSONException e) {
			throw new TwitterException.Parsing(result, e);
		} catch(TwitterException.E403 e) {
			// repeated DMs get a 403
			if (e.getMessage()!=null && e.getMessage().startsWith("code 151:")) {
				throw new TwitterException.Repetition("DM "+recipient+" "+text+" Error:"+e);
			}
			throw e;
		} catch (TwitterException.E404 e) {
			// Probably a suspended user. But could be a rename or a delete.
			throw new TwitterException.MissingUser(e.getMessage() + " with recipient="
					+ recipient + ", text=" + text);
		}
	}

	/**
	 * Set this to access sites other than Twitter that support the Twitter API.
	 * E.g. WordPress or Identi.ca. Note that not all methods may work! Also,
	 * search uses a separate url and is not affected by this method (it will
	 * continue to point to Twitter).
	 * 
	 * @param url
	 *            Format: "http://domain-name", e.g. "http://twitter.com" by
	 *            default. Or https
	 */
	public void setAPIRootUrl(String url) {
		assert url.startsWith("http://") || url.startsWith("https://") : url;
		assert !url.endsWith("/") : "Please remove the trailing / from " + url;
		TWITTER_URL = url;
	}

	/**
	 * *Some* methods - the timeline ones for example - allow a count of
	 * number-of-tweets to return.
	 * 
	 * @param count
	 *            null for default behaviour. 200 is the current maximum.
	 *            Twitter may reject or ignore high counts.
	 */
	public void setCount(Integer count) {
		this.count = count;
	}

	/**
	 * 
	 * @param status The status to favorite. Technical note: Only the ID is needed, so you can use a "fake" Status object here. 
	 * @param isFavorite
	 * @return updated Status, or null if you'd already starred this status. 
	 */
	public Status setFavorite(Status status, boolean isFavorite) {
		try {
			String uri = isFavorite ? TWITTER_URL + "/favorites/create.json" 
					: TWITTER_URL + "/favorites/destroy.json";
			String json = http.post(uri, InternalUtils.asMap("id", status.id), true);
			return new Status(new JSONObject(json), null);
		} catch (E403 e) {
			// already a favorite?
			if (e.getMessage() != null
					&& e.getMessage().contains("already favorited")) {
				return null;
			}
			// just a normal 403
			throw e;
		}
	}

	/**
	 * true by default. If true, lists of tweets will include new-style
	 * retweets. If false, they won't (execpt for the retweet-specific calls).
	 * 
	 * @param includeRTs
	 */
	public void setIncludeRTs(boolean includeRTs) {
		this.includeRTs = includeRTs;
	}

	/**
	 * Note: does NOT work for search() methods (not supported by Twitter).
	 * 
	 * @param tweetEntities
	 *            Set to true to enable
	 *            {@link Status#getTweetEntities(KEntityType)}, false if you
	 *            don't care. Default is true.
	 */
	public void setIncludeTweetEntities(boolean tweetEntities) {
		this.tweetEntities = tweetEntities;
	}

	/**
	 * Set a language filter for search results. Note: This only applies to
	 * search results.
	 * 
	 * @param language
	 *            ISO code for language. Can be null for all languages.
	 *            <p>
	 *            Note: there are multiple different ISO codes! Twitter supports
	 *            ISO 639-1. http://en.wikipedia.org/wiki/ISO_639-1
	 */
	public void setLanguage(String language) {
		lang = language;
	}

	/**
	 * @param maxResults
	 *            if greater than zero, requests will attempt to fetch as many
	 *            pages as are needed! -1 by default, in which case most methods
	 *            return the first 20 statuses/messages. Zero is not allowed.
	 *            <p>
	 *            If setting a high figure, you should usually also set a
	 *            sinceId or sinceDate to limit your Twitter usage. Otherwise
	 *            you can easily exceed your rate limit.
	 */
	public void setMaxResults(int maxResults) {
		assert maxResults != 0;
		this.maxResults = maxResults;
	}

	/**
	 * Set the location for your tweets.<br>
	 * 
	 * Warning: geo-tagging parameters are ignored if geo_enabled for the user
	 * is false (this is the default setting for all users unless the user has
	 * enabled geolocation in their settings)!
	 * 
	 * @param latitudeLongitude
	 *            Can be null (which is the default), in which case your tweets
	 *            will not carry location data.
	 *            <p>
	 *            The valid ranges for latitude is -90.0 to +90.0 (North is
	 *            positive) inclusive. The valid ranges for longitude is -180.0
	 *            to +180.0 (East is positive) inclusive.
	 * 
	 * @see #setSearchLocation(double, double, String) which is completely
	 *      separate.
	 */
	public void setMyLocation(double[] latitudeLongitude) {
		myLatLong = latitudeLongitude;
		if (myLatLong == null)
			return;
		if (Math.abs(myLatLong[0]) > 90)
			throw new IllegalArgumentException(myLatLong[0]
					+ " is not within +/- 90");
		if (Math.abs(myLatLong[1]) > 180)
			throw new IllegalArgumentException(myLatLong[1]
					+ " is not within +/- 180");
	}

	/**
	 * Restricts {@link #search(String)} to tweets by users located within a
	 * given radius of the given latitude/longitude.
	 * <p>
	 * The location of a tweet is preferably taken from the Geotagging API, but
	 * will fall back to the Twitter profile.
	 * 
	 * @param latitude
	 * @param longitude
	 * @param radius
	 *            E.g. 3.5mi or 2km. Must be <2500km
	 */
	public void setSearchLocation(double latitude, double longitude,
			String radius) {
		assert radius.endsWith("mi") || radius.endsWith("km") : radius;		
		geocode = ((float)latitude) + "," + ((float)longitude) + "," + radius;
	}

	/**
	 * @return latitude,longitude,radius
	 */
	public String getSearchLocation() {
		return geocode;
	}
	
	/**
	 * Optional. Specifies what type of search results you would prefer to
	 * receive. The current default is "mixed." Valid values:<br>
	 * {@link #SEARCH_MIXED}: Include both popular and real time results in the
	 * response.<br> {@link #SEARCH_RECENT}: return only the most recent results in
	 * the response<br> {@link #SEARCH_POPULAR}: return only the most popular
	 * results in the response.<br>
	 * 
	 * @param resultType
	 */
	public void setSearchResultType(String resultType) {
		this.resultType = resultType;
	}

	/**
	 * Date based filter on statuses and messages. This is done client-side as
	 * Twitter have - for their own inscrutable reasons - pulled support for
	 * this feature. Use {@link #setSinceId(Number)} for preference.
	 * You can use both constraints together.
	 * <p>
	 * If using this, you probably also want to increase
	 * {@link #setMaxResults(int)} - otherwise you get at most 20, and possibly
	 * less (since the filtering is done client side).
	 * 
	 * @param sinceDate
	 * @see #setSinceId(Number)
	 */
	@Deprecated
	public void setSinceDate(Date sinceDate) {
		this.sinceDate = sinceDate;
	}

	/**
	 * Narrows the returned results to just those statuses created after the
	 * specified status id. This will be used until it is set to null. The default
	 * is null.
	 * <p>
	 * If using this, you probably also want to use {@link #setUntilId(Number)}.
	 * Twitter returns the most recent results, so this has little effect unless 
	 * used with setUntilId().
	 * You may also want to increase {@link #setMaxResults(int)}.
	 * 
	 * @param statusId Can be null. Only a BigInteger really makes sense (although a double would work to some degree
	 * -- but beware of rounding errors).
	 * @see #setSinceDate(Date)
	 */
	public void setSinceId(Number statusId) {
		sinceId = InternalUtils.toBigInteger(statusId);
	}

	/**
	 * Set the source application. This will be mentioned on Twitter alongside
	 * status updates (with a small label saying source: myapp).
	 * 
	 * <i>In order for this to work, you must first register your app with
	 * Twitter and get a source name from them! You must also use OAuth to
	 * connect.</i>
	 * 
	 * @param sourceApp
	 *            jtwitterlib by default. Set to null for no source.
	 */
	public void setSource(String sourceApp) {
		this.sourceApp = sourceApp;
	}

	/**
	 * Sets the authenticating user's status.
	 * <p>
	 * Identical to {@link #updateStatus(String)}, but with a Java-style name
	 * (updateStatus is the Twitter API name for this method).
	 * 
	 * @param statusText
	 *            The text of your status update. Must not be more than 140
	 *            characters.
	 * @return The posted status when successful.
	 */
	public Status setStatus(String statusText) throws TwitterException {
		return updateStatus(statusText);
	}

	/**
	 * @param untilDate
	 *            the untilDate to set. This is NOT
	 *            properly supported. It operates by post filtering
	 *            results client-side.
	 * @see #setUntilId(Number) which is better
	 */
	@Deprecated
	public void setUntilDate(Date untilDate) {
		this.untilDate = untilDate;
	}

	/**
	 * If set, return results older than this.
	 * 
	 * @param untilId
	 *            aka max_id
	 */
	public void setUntilId(Number untilId) {
		this.untilId = InternalUtils.toBigInteger(untilId);
	}
	
	public BigInteger getUntilId() {
		return untilId;
	}
	
	public BigInteger getSinceId() {
		return sinceId;
	}

	/**
	 * @deprecated User {@link TwitLonger}
	 * Keeping for backwards compatibility until Q2 2013
	 *  
	 * Set this to allow the use of twitlonger via
	 * {@link #updateLongStatus(String, long)}. To get an api-key for your app,
	 * contact twitlonger as described here: http://www.twitlonger.com/api
	 * 
	 * This method will be removed June 2013
	 * 
	 * @param twitlongerAppName
	 * @param twitlongerApiKey
	 */
	public void setupTwitlonger(String twitlongerAppName,
			String twitlongerApiKey) {
		this.twitlongerAppName = twitlongerAppName;
		this.twitlongerApiKey = twitlongerApiKey;
	}

	/**
	 * @see Twitter_Users#show(Number)
	 * 
	 * This method will be removed June 2013
	 */
	@Deprecated
	public User show(Number userId) {
		return users().show(userId);
	}

	/**
	 * @see Twitter_Users#show(String)
	 */
	@Deprecated
	public User show(String screenName) throws TwitterException,
			TwitterException.SuspendedUser {
		return users().show(screenName);
	}

	/**
	 * Split a long message up into shorter chunks suitable for use with
	 * {@link #setStatus(String)} or {@link #sendMessage(String, String)}.
	 * 
	 * @param longStatus
	 * @return longStatus broken into a list of max 140 char strings
	 */
	public List<String> splitMessage(String longStatus) {
		// Is it really long?
		if (longStatus.length() <= 140)
			return Collections.singletonList(longStatus);
		// Multiple tweets for a longer post
		List<String> sections = new ArrayList<String>(4);
		StringBuilder tweet = new StringBuilder(140);
		String[] words = longStatus.split("\\s+");
		for (String w : words) {
			// messages have a max length of 140
			// plus the last bit of a long tweet tends to be hidden on
			// twitter.com, so best to chop 'em short too
			if (tweet.length() + w.length() + 1 > 140) {
				// Emit
				tweet.append("...");
				sections.add(tweet.toString());
				tweet = new StringBuilder(140);
				tweet.append(w);
			} else {
				if (tweet.length() != 0) {
					tweet.append(" ");
				}
				tweet.append(w);
			}
		}
		// Final bit
		if (tweet.length() != 0) {
			sections.add(tweet.toString());
		}
		return sections;
	}

	/**
	 * Map with since_id, page and count, if set. This is called by methods that
	 * return lists of statuses or messages.
	 */
	private Map<String, String> standardishParameters() {
		return addStandardishParameters(new HashMap<String, String>());
	}

	/**
	 * @see Twitter_Users#stopFollowing(String)
	 * 
	 * This method will be removed June 2013
	 */
	@Deprecated
	public User stopFollowing(String username) {
		return users().stopFollowing(username);
	}

	/**
	 * @see Twitter_Users#stopFollowing(User)
	 * 
	 * This method will be removed June 2013
	 */
	@Deprecated
	public User stopFollowing(User user) {
		return stopFollowing(user.screenName);
	}

	/**
	 * Update info on Twitter's configuration -- such as shortened url lengths.
	 * @return true if we detected a change from the hardcoded defaults.
	 */
	public boolean updateConfiguration() {
		String json = http.getPage(TWITTER_URL + "/help/configuration.json",
				null, true);
		boolean change = false;
		try {
			JSONObject jo = new JSONObject(json);
			{
				int len = jo.getInt("short_url_length");;
				if (len != LINK_LENGTH) change = true;
				LINK_LENGTH = len;
			}
			// max_media_per_upload // 1!
			{
				int len = jo.getInt("characters_reserved_per_media");
				if (len != MEDIA_LENGTH) change = true;
				MEDIA_LENGTH = len;
			}
			{	
				long lmt = jo.getLong("photo_size_limit");
				if (lmt != PHOTO_SIZE_LIMIT) change = true;
				PHOTO_SIZE_LIMIT = lmt;
			}
			// photo_sizes
			// short_url_length_https
			return change;
		} catch (JSONException e) {
			throw new TwitterException.Parsing(json, e);
		}
	}

	/**
	 * @deprecated Use {@link TwitLonger}  
	 * Keeping for backwards compatibility until Q2 2013
	 * 
	 * Use twitlonger.com to post a lengthy tweet. See twitlonger.com for more
	 * details on their service.
	 * <p>
	 * Note: You need to have called {@link #setupTwitlonger(String, String)}
	 * before calling this.
	 * 
	 * @param message
	 * @param inReplyToStatusId
	 *            Can be null if this isn't a reply
	 * @return A Twitter status using a truncated message with a link to
	 *         twitlonger.com
	 * @see #setupTwitlonger(String, String)
	 */
	public Status updateLongStatus(String message, Number inReplyToStatusId) {
		TwitLonger tl = new TwitLonger(this, twitlongerApiKey, twitlongerAppName);
		return tl.updateLongStatus(message, inReplyToStatusId);
	}

	/**
	 * Updates the authenticating user's status.
	 * 
	 * @param statusText
	 *            The text of your status update. Must not be more than 140 characters.
	 * @return The posted status when successful.
	 */
	public Status updateStatus(String statusText) {
		return updateStatus(statusText, null);
	}
	
	
	
	/**
	 * Compute the effective size of a message, given that Twitter treats things that
	 * smell like a URL as 23 characters.
	 * This also checks for DM microformat, e.g. "d winterstein Hello", where the d user part isn't counted.
	 * 
	 * @param statusText
	 * 			The status to check 
	 * @return 
	 * 			The effective message length in characters
	 */
	public static int countCharacters(String statusText) {
		int shortLength = statusText.length();	
		// Urls count as 23
		Matcher m =  Regex.VALID_URL.matcher(statusText);
		while(m.find()) {
			String grp = m.group();
			shortLength += LINK_LENGTH - grp.length();
		}
		// If a DM, don't count the "d user" microformat
		Matcher dmm = InternalUtils.DM.matcher(statusText);		
		if (dmm.find()) {
			shortLength -= dmm.end();			
		}

		return shortLength;
	}
	
	/**
	 * Updates the authenticating user's status and marks it as a reply to the
	 * tweet with the given ID.
	 * 
	 * @param statusText
	 *            The text of your status update. Must not be more than 140
	 *            characters (with urls counting as 20 or 21 for https).
	 * 
	 * 
	 * @param inReplyToStatusId
	 *            The ID of the tweet that this tweet is in response to. The
	 *            statusText must contain the username (with an "@" prefix) of
	 *            the owner of the tweet being replied to for Twitter to
	 *            agree to mark the tweet as a reply. <i>null</i> to leave this
	 *            unset.
	 * 
	 * @return The posted status when successful.
	 *         <p>
	 *         Warning: the microformat for direct messages is supported. BUT:
	 *         the return value from this method will be null, and not the
	 *         direct message. Other microformats (such as follow) may result in
	 *         an exception being thrown.
	 * 
	 * @throws TwitterException
	 *             if something goes wrong. There is a rare (but not rare
	 *             enough) bug whereby Twitter occasionally returns a success
	 *             code but the wrong tweet. If this happens, the update may or
	 *             may not have worked - wait a bit & check.
	 */
	public Status updateStatus(String statusText, Number inReplyToStatusId)
			throws TwitterException 
	{		
		Map<String, String> vars = updateStatus2_vars(statusText, inReplyToStatusId, false);
		String result = http.post(TWITTER_URL + "/statuses/update.json", vars,
					true);
		try {
			Status s = new Status(new JSONObject(result), null);
//			s = updateStatus2_safetyCheck(statusText, s);
			return s;
		} catch (JSONException e) {
			throw new TwitterException.Parsing(result, e);
		}
	}

	/**
	 * Check statusText length & prep the parameters
	 * @param statusText
	 * @param inReplyToStatusId
	 * @return The vars to send
	 */
	private Map<String, String> updateStatus2_vars(String statusText, Number inReplyToStatusId, boolean withMedia) 
	{
		// check for length
		int max = withMedia? MAX_CHARS - MEDIA_LENGTH : MAX_CHARS;
		if (statusText.length() > max 
				&& TWITTER_URL.contains("twitter") // Hack: allow long posts to WordPress
				&& CHECK_TWEET_LENGTH)  
		{
			int shortLength = countCharacters(statusText);
			if (shortLength > max) {
				// bogus - send a helpful error
				if (statusText.startsWith("RT")) {
					throw new IllegalArgumentException(
							"Status text must be 140 characters or less -- use Twitter.retweet() to do new-style retweets which can be a bit longer: "
									+ statusText.length() + " " + statusText);
				}
				if (withMedia) {
					throw new IllegalArgumentException(
							"Status-with-media text must be "+max+" characters or less: "
									+ statusText.length() + " " + statusText);
				}
				throw new IllegalArgumentException(
						"Status text must be 140 characters or less: "
								+ statusText.length() + " " + statusText);
			}
		}
		
		Map<String, String> vars = InternalUtils.asMap("status", statusText);
		if (tweetEntities) vars.put("include_entities", "1");

		// add in long/lat if set
		if (myLatLong != null) {
			vars.put("lat", Double.toString(myLatLong[0]));
			vars.put("long", Double.toString(myLatLong[1]));
		}
		if (placeId != null) {
			vars.put("place_id", Long.toString(placeId));
		}

		if (sourceApp != null) {
			vars.put("source", sourceApp);
		}
		if (inReplyToStatusId != null) {
			// TODO remove this legacy check
			double v = inReplyToStatusId.doubleValue();
			assert v != 0 && v != -1;
			vars.put("in_reply_to_status_id", inReplyToStatusId.toString());
		}
		return vars;
	}

//	/**
//	 * Test that the updateState worked -- throw TwitterException.Unexplained
//	 * if it didn't.<br>
//	 * By default, this only filters DMs.<br>
//	 * Serious checking is switched on via the {@link #WORRIED_ABOUT_TWITTER} flag.
//	 * @param statusText What we meant to send
//	 * @param s What came back
//	 * @return s, or null for DMs
//	 * @throws TwitterException#Unexplained 
//	 */
//	private Status updateStatus2_safetyCheck(String statusText, Status s) {
//		// is it a direct message? - which doesn't return the true status
//		String st = statusText.toLowerCase();
//		if (st.startsWith("dm ") || st.startsWith("d ")) {
//			return null;
//		}
//		// The checks are dialled down, so let's make this standard
////		if ( ! WORRIED_ABOUT_TWITTER) {
////			return s;
////		}
//		// Weird bug: Twitter occasionally rejects tweets?!
//		// Sightings...
//		// 21/05/12 (spotter: Alex Nuttgens)
//		// 27/03/12 (spotter: Alex Nuttgens)
//		// + other earlier sightings
//		
//		// Bug #6748: Unicode mangling *sometimes*
//		
//		// Sanity check...
//		String targetText = statusText.trim();
//		String returnedStatusText = s.text.trim();
//		// strip the urls to remove the effects of the t.co shortener
//		// (obviously this weakens the safety test, but failure would be
//		// a corner case of a corner case).
//		// TODO Twitter also shorten some not-quite-urls, such as "www.google.com", which stripUrls() won't catch.		
//		targetText = InternalUtils.stripUrls(targetText);
//		returnedStatusText = InternalUtils.stripUrls(returnedStatusText);
//		if (returnedStatusText.equals(targetText)) {
//			// All OK
//			return s;
//		}
//		InternalUtils.log("jtwitter", "Text mismatch: "+targetText+" != "+returnedStatusText+" tweet:"+s.getId());
//		return s;
//		
//		// More extreme measures... off for now
////		try {
////			Thread.sleep(500);
////		} catch (InterruptedException e) {
////			// igore the interruption
////		}
////		Status s2 = getStatus();			
////		if (s2 != null) {
////			returnedStatusText = InternalUtils.stripUrls(s2.text.trim());
////			if (targetText.equals(returnedStatusText)) {			
////				return s2;
////			}
////		}
////		throw new TwitterException.Unexplained(
////				"Unexplained failure for tweet: expected \"" + statusText
////						+ "\" but got " + s2);
//	}


	/**
	 * Updates the user's status with an image (or other media file / attachment).
	 * 
	 * @param statusText
	 * @param inReplyToStatusId Can be null.
	 * @param mediaFile
	 * @return The posted status when successful.
	 * 
	 * @see #PHOTO_SIZE_LIMIT
	 */
	// c.f. https://dev.twitter.com/docs/api/1/post/statuses/update_with_media 	
	// c.f. https://dev.twitter.com/discussions/1059
	public Status updateStatusWithMedia(String statusText, BigInteger inReplyToStatusId, File mediaFile) {
		if (mediaFile==null || ! mediaFile.isFile()) {
			throw new IllegalArgumentException("Invalid file: "+mediaFile);
		}		
		Map vars = updateStatus2_vars(statusText, inReplyToStatusId, true);
		vars.put("media[]", mediaFile);
		// TODO possibly_sensitive
		// TODO display_coordinates
		String result = null;
		try {			
			// Breaking change from v1.0, which went to upload.twitter.com
			String url = TWITTER_URL+"/statuses/update_with_media.json";
			result = ((OAuthSignpostClient)http).postMultipartForm(url, vars);
			Status s = new Status(new JSONObject(result), null);
			// sanity check (c.f. unicode bug #6748)
//			updateStatus2_safetyCheck(statusText, s);
			return s;
		} catch (E403 e) {
			// test for repetition (which gets a 403)
			Status s = getStatus();
			if (s != null && s.getText().equals(statusText))
				throw new TwitterException.Repetition(s.getText());
			throw e;
		} catch (JSONException e) {
			throw new TwitterException.Parsing(result, e);
		}
	}
	
	/**
	 * Updates the user's status with multiple images.
	 * 
	 * @param statusText
	 * @param inReplyToStatusId Can be null.
	 * @param mediaFiles
	 * @return The posted status when successful.
	 * 
	 * @see #PHOTO_SIZE_LIMIT
	 */
	// c.f. https://dev.twitter.com/docs/api/1/post/statuses/update_with_media 	
	// c.f. https://dev.twitter.com/discussions/1059
	public Status updateStatusWithMedia(String statusText, BigInteger inReplyToStatusId, List<File> mediaFiles) {
		// Upload each file, and get a media_id for it
		// List.toString() outputs "[item1, ..., itemN]" but Twitter wants "item1,...,itemN" so we do it the hard way 
		StringBuilder fileIds = new StringBuilder();
		for (File file: mediaFiles) {
			if (file == null || ! file.isFile()) {
				throw new IllegalArgumentException("Invalid file: " + file);
			}
			
			Map vars = new HashMap();
			vars.put("media", file);
			String url = TWITTER_UPLOAD_URL + MEDIA_UPLOAD_ENDPOINT;
			String result = ((OAuthSignpostClient)http).postMultipartForm(url, vars);
			JSONObject response = new JSONObject(result);
			String id = response.optString("media_id_string");
			if (id != null) fileIds.append(id + ",");
		}
		
		Map vars = updateStatus2_vars(statusText, inReplyToStatusId, true);
		if(fileIds.length() > 0) vars.put("media_ids", fileIds.substring(0, fileIds.length() - 1));
		
		String result = null;
		try {			
			// Breaking change from v1.0, which went to upload.twitter.com
			String url = TWITTER_URL+"/statuses/update.json";
			result = http.post(url, vars,
					true);
			
			Status s = new Status(new JSONObject(result), null);

			return s;
		} catch (E403 e) {
			// test for repetition (which gets a 403)
			Status s = getStatus();
			if (s != null && s.getText().equals(statusText))
				throw new TwitterException.Repetition(s.getText());
			throw e;
		} catch (JSONException e) {
			throw new TwitterException.Parsing(result, e);
		}
	}

	/**
	 * User and social-network related API methods.
	 * <p>
	 * Note: this is a new object with an unset cursor.
	 */
	public Twitter_Users users() {
		return new Twitter_Users(this);
	}

	/**
	 * 
	 * @param place Can be null (switches off geo-filtering)
	 */
	public void setSearchLocation(IPlace place) {
		if (place==null) {
			geocode = null;
			return;
		}
		Location x = place.getCentroid();
		if (x==null) {
			throw new IllegalArgumentException("Geo-search needs lat/long coordinates - none in "+place);
		}
		BoundingBox bbox = place.getBoundingBox();
		if (bbox==null) {
			// default to 10km radius = a 20km diameter
			setSearchLocation(x.latitude, x.longitude, "10km");
		}
		Location ne = bbox.getNorthEast();
		Location sw = bbox.getSouthWest();
		assert ne != null && sw != null : bbox;
		// cast down to m to reduce the sig-figures
		float diameterInMetres = (int) ne.distance(sw).getValue();
		float radiusInKm = Math.max(diameterInMetres/2000, 5000);
		// Warning: cap radius at 2500 (limit imposed by Twitter)
		radiusInKm = Math.min(radiusInKm, 2499);
		String radiusInGeoCodeKm = radiusInKm + "km";		
		setSearchLocation(x.latitude, x.longitude, radiusInGeoCodeKm);		
	}

	/**
	 * Convenience for using getHttpClient().getRateLimits()
	 * @param apiResourceName
	 * @param defaultIfUnset If the answer is not known, return this value.
	 * ie. true for conservative behaviour (beware of never getting data), false for optimistic behaviour.
	 * @return true if rate-limited
	 */
	public boolean isRateLimited(String apiResourceName, boolean defaultIfUnset) {
		Map<String, RateLimit> ratelimits = getHttpClient().getRateLimits();
		if (ratelimits==null) return defaultIfUnset;
		RateLimit rl = ratelimits.get(apiResourceName);
		if (rl==null) return defaultIfUnset;
		if (rl.isOutOfDate()) return defaultIfUnset;
		return rl.getRemaining() <= 0;
	}


}
