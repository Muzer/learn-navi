package org.learnnavi.app;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.SearchManager;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Adapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.ArrayAdapter;
import android.widget.TextView;


public class Dictionary extends ListActivity implements OnClickListener {
	private final int ENTRY_DIALOG = 100; 
	
	static private Button mToNaviButton;
	static private String mCurSearch;
	static private String mCurSearchNavi;

	static private final int DELETE_WEIGHT = 1;
	static private final int INSERT_WEIGHT = 1;
	static private final int SUBSTITUTE_WEIGHT = 2;
	static private final int TRANSPOSITION_WEIGHT = 1;
	
	private int mViewingItem;
	
	private boolean mDbIsOpen = false;
	private boolean mToNavi = false;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);

	    // Set it to typing will open up a search box
        setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);

        setContentView(R.layout.dictionary);

        // Setup the handler for clicking cancel button (Need a better way to present this)
        Button cancelsearch = (Button)findViewById(R.id.CancelSearch);
        cancelsearch.setOnClickListener(this);

        // Setup the handler for clicking the dictionary direction button
        mToNaviButton = (Button)findViewById(R.id.DictionaryType);
        mToNaviButton.setOnClickListener(this);

        // Check if this was created as the result of a search (Shouldn't happen currently)
        checkIntentForSearch(getIntent());

        // Open the DB
	    EntryDBAdapter.getInstance(this).openDataBase();
	    mDbIsOpen = true;

	    // Check if there is saved state and restore it if so
	    if (savedInstanceState != null)
	    {
	    	if (savedInstanceState.containsKey("CurSearch"))
	    		mCurSearch = savedInstanceState.getString("CurSearch");
	    	if (savedInstanceState.containsKey("CurSearchNavi"))
	    		mCurSearchNavi = savedInstanceState.getString("CurSearchNavi");
	    	if (savedInstanceState.containsKey("ToNavi"))
	    		mToNavi = savedInstanceState.getBoolean("ToNavi");
	    	if (savedInstanceState.containsKey("ViewingItem"))
	    		mViewingItem = savedInstanceState.getInt("ViewingItem");
	    }

	    // Populate the list
	    fillData();
	}
	
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState)
	{
		// Save the search and direction
		// Future: Save the part of speech filter
		// Other things like position of the scroll is handled automatically
		super.onSaveInstanceState(savedInstanceState);

		savedInstanceState.putString("CurSearch", mCurSearch);
		savedInstanceState.putString("CurSearchNavi", mCurSearchNavi);
		savedInstanceState.putBoolean("ToNavi", mToNavi);
		savedInstanceState.putInt("ViewingItem", mViewingItem);
	}
	
	@Override
	public void onStart()
	{
		// Set the search type according to the current setting when the activity starts
		super.onStart();
		NaviWords.setSearchType(mToNavi);
	}
	
	@Override
	public void onStop()
	{
		// Clear the search type when the activity stops
		super.onStop();
		NaviWords.setSearchType(null);	
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.dictionary_menu, menu);
	    return true;
	}
	
	/* Handles item selections */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	    case R.id.Search:
	    	return onSearchRequested();
	    }
	    return false;
	}	
	
	@Override
	public boolean onSearchRequested()
	{
		// Load up the current search term when the search dialog opens
    	startSearch(getCurSearch(), true, null, false);
    	return true;
	}

	@Override
	protected void onNewIntent(Intent intent)
	{
		// The result of a search, process the resulting search request
		if (checkIntentForSearch(intent))
			fillData();
	}
	
	private void setCurSearch(String search)
	{
		// Set the search term, keeping in mind which direction is being searched
			mCurSearch = search;
	}
	
	private String getCurSearch()
	{
		// Get the search term, keeping in mind which direction is being searched
			return mCurSearch;
	}
	
	private boolean checkIntentForSearch(Intent intent)
	{
		if (Intent.ACTION_VIEW.equals(intent.getAction()))
		{
			// View - A suggestion was selected, just open the entry directly
			String id = intent.getDataString();
			try
			{
				showDialogForId(Integer.parseInt(id));
			}
			catch (Exception ex)
			{
			}
			// Don't actually perform a search
			return false;
		}
		if (Intent.ACTION_SEARCH.equals(intent.getAction()))
		{
			// Set (Or clear) the search term from the intent
			String search = intent.getStringExtra(SearchManager.QUERY);
			if (search == "")
				search = null;
			setCurSearch(search);
			// Return true to indicate that it should perform the search
			return true;
		}
		else
		{
			// Don't know what intent is requested, so ignore it
			return false;
		}
	}

	//Calculates the Damereau-levenshtein distance between the strings
	private Integer getDistance(String m, String n) {
		int mLength = m.length();
		int nLength = n.length();

		//Some optimizations
		if (mLength == 1) return nLength;
		if (nLength == 1) return mLength;

		int[][] matrix = new int[mLength+1][nLength+1];
		
		for(int i=0; i<=mLength; i++) {
			matrix[i][0] = i;
		}
		for(int j=0; j<=nLength; j++) {
			matrix[0][j] = j;
		}

		for (int i=1; i<=mLength; i++) {
			for (int j=1; j<=nLength; j++) {
				int cost = (n.charAt(j-1) == m.charAt(i-1)) ? 0 : 1;
				int tmp[] = { (matrix[i-1][j] + 1), //delete
							(matrix[i][j-1] + 1), //insert
							(matrix[i-1][j-1] + cost)}; //substition
				Arrays.sort(tmp);
				matrix[i][j] = tmp[0]; //gets the minimum value

				if (i > 1 && j > 1 && (m.charAt(i-1) == n.charAt(j-2)) && (m.charAt(i-2) == n.charAt(j-1))) {
					int s = matrix[i][j];
					int t = (matrix[i-2][j-2] + cost); //transpostion
					matrix[i][j] = (s < t) ? s : t; 
				}
			}
		}
		return matrix[mLength][nLength];
	}

    private void fillData() {
    	Cursor ciN, ciE;

    	String cursearch = getCurSearch();
    	
		Button cancel = (Button)findViewById(R.id.CancelSearch);
    	if (cursearch != null)
    	{
    		// Set the text on the button to reflect the search
    		cancel.setVisibility(Button.VISIBLE);
    		cancel.setText(getString(R.string.CancelSearch).replace("$F$", cursearch));
    	}
    	else {
    		// Hide the button, no search active
    		cancel.setVisibility(Button.GONE);
			return;
		}
    	
    	// Query the dictionary to Na'vi
    	mToNaviButton.setText("Both");
		//cursor in x
      	ciN = EntryDBAdapter.getInstance(this).queryAllEntries(cursearch.substring(0,1));
      	ciE = EntryDBAdapter.getInstance(this).queryAllEntriesToNavi(cursearch.substring(0,1));

		ArrayList<WordStruct> results = new ArrayList<WordStruct>();

		int wordId = ciN.getColumnIndex(EntryDBAdapter.getInstance(this).KEY_WORD);
		int defId = ciN.getColumnIndex(EntryDBAdapter.getInstance(this).KEY_DEFINITION);
		//Word is in Na'vi 
		//Def is in English
		if(ciN.moveToFirst()) {
			do {
					String def = ciN.getString(defId);
					String word = ciN.getString(wordId);
					Integer dis = getDistance(word, cursearch);
					//if (dis < THRESHOLD)
						results.add(new WordStruct(dis, def+Integer.toString(dis), word));

			} while (ciN.moveToNext());
		}
		wordId = ciE.getColumnIndex(EntryDBAdapter.getInstance(this).KEY_WORD);
		defId = ciE.getColumnIndex(EntryDBAdapter.getInstance(this).KEY_DEFINITION);
		//word is in english
		//def is in Na'vi
		if(ciE.moveToFirst()) {
			do {
					String def = ciE.getString(defId);
					String word = ciE.getString(wordId);
					if (word.contains(",") && !word.contains("(")) {
						String words[] = word.split(",");
						for(String subWord : words) {
						  	String subWordT = subWord.trim();
						  	if(subWordT.substring(0,1) != cursearch.substring(0,1))
							  continue;

							Integer dis = getDistance(subWordT, cursearch);
							//if (dis < THRESHOLD)
								results.add(new WordStruct(dis, def+Integer.toString(dis), subWordT));
						}
					} 
					else {
						Integer dis = getDistance(word, cursearch);
						//if (dis < THRESHOLD)
							results.add(new WordStruct(dis, def+Integer.toString(dis), word));
					}
			} while (ciE.moveToNext());
		}

		SearchResultsAdapter entries = new SearchResultsAdapter(this, results);
		setListAdapter(entries);
    }
    
	private void fillFields(int rowid, Dialog d) {
		Cursor entry = EntryDBAdapter.getInstance(this).querySingleEntry(rowid);

		// Nothing to actually display - should never happen
		if (!entry.moveToFirst())
			return;

		// Pad the word with spaces so it doesn't get cut off
		TextView text = (TextView)d.findViewById(R.id.EntryWord);
		text.setText(entry.getString(entry.getColumnIndexOrThrow(EntryDBAdapter.KEY_WORD)) + "  ");
		
		text = (TextView)d.findViewById(R.id.EntryPartOfSpeech);
		text.setText(entry.getString(entry.getColumnIndexOrThrow(EntryDBAdapter.KEY_PART)));

		text = (TextView)d.findViewById(R.id.EntryIPA);
		String ipastr = entry.getString(entry.getColumnIndexOrThrow(EntryDBAdapter.KEY_IPA));
		// Replace the ejective marker from ' to something more visible in the IPA font, and add [] around the text
		if (ipastr != null && ipastr != "")
			ipastr = "[" + ipastr.replace('\'', '\u02bc') + "]";
		text.setText(ipastr);

		text = (TextView)d.findViewById(R.id.EntryDefinition);
		text.setText(entry.getString(entry.getColumnIndexOrThrow(EntryDBAdapter.KEY_DEFINITION)));
	}
	
	private void showDialogForId(int id)
	{
		mViewingItem = id;
		showDialog(ENTRY_DIALOG);
	}
	
	@Override
	protected void onPrepareDialog(int id, Dialog dialog)
	{
		super.onPrepareDialog(id, dialog);

		if (id == ENTRY_DIALOG)
		{
	        Button done = (Button)dialog.findViewById(R.id.DoneButton);
	        done.setOnClickListener(this);

	        fillFields(mViewingItem, dialog);
		}
	}
	
	@Override
	protected Dialog onCreateDialog(int id)
	{
		if (id == ENTRY_DIALOG)
		{
			Dialog ret = new Dialog(this, android.R.style.Theme_Dialog);
			ret.requestWindowFeature(Window.FEATURE_NO_TITLE);
			ret.setContentView(R.layout.dictionary_entry);
            Button done = (Button)ret.findViewById(R.id.DoneButton);
            done.setOnClickListener(this);
            
            // Load a custom font for the IPA string
    		TextView text = (TextView)ret.findViewById(R.id.EntryIPA);
    		text.setTypeface(Typeface.createFromAsset(getAssets(), "ipafont.ttf"));
    		
	        fillFields(mViewingItem, ret);
    		
    		return ret;
		}
		
		return super.onCreateDialog(id);
	}
	
	@Override
    protected void onListItemClick (ListView l, View v, int position, long id) {
    	super.onListItemClick (l, v, position, id);

    	if (id >= 0)
    	{
	    	// Show the entry
    		showDialogForId((int)id);
    	}
    }
    
	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.DoneButton)
		{
			this.dismissDialog(ENTRY_DIALOG);
			return;
		}
		else if (v.getId() == R.id.CancelSearch)
		{
			setCurSearch(null);
			fillData();
			return;
		}
		
		mToNavi = !mToNavi;
		NaviWords.setSearchType(mToNavi);	
		fillData();
	}

	@Override
	public void onDestroy()
	{
		if (mDbIsOpen)
		{
			mDbIsOpen = false;
			EntryDBAdapter.getInstance(this).close();
		}
		super.onDestroy();
	}
}
