package org.ludmann.minimalisticcalendargrid.app;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.TimeZone;


public class EventsView extends Activity implements LoaderManager.LoaderCallbacks<Cursor> {
    /**
     * The event fields to query.
     */
    private static final String[] EVENTS_PROJECTION = new String[]{
            CalendarContract.Events._ID,                // 0
            CalendarContract.Events.TITLE,              // 1
            CalendarContract.Events.EVENT_LOCATION,     // 2
            CalendarContract.Events.DTSTART,            // 3
            CalendarContract.Events.DTEND,              // 4
            CalendarContract.Events.ALL_DAY,            // 5
            CalendarContract.Events.DELETED,            // 6
            CalendarContract.Events.VISIBLE,            // 7
            CalendarContract.Events.DISPLAY_COLOR       // 8
    };
    /**
     * The selection string for the events.
     * dtstart <= end timestamp
     * AND
     * dtend >= start timestamp
     * AND
     * visible = true
     * AND
     * deleted = false
     */
    private static final String EVENTS_SELECTION = "(" +
            CalendarContract.Events.DTSTART + " <= ?) AND (" +
            CalendarContract.Events.DTEND + " >= ?) AND (" +
            CalendarContract.Events.VISIBLE + "=1) AND (" +
            CalendarContract.Events.DELETED + "=0)";
    /**
     * start timestamp key for bundle
     */
    private static final String START_TS = "startTS";
    /**
     * end timestamp key for bundle
     */
    private static final String END_TS = "endTS";


    private int day, month, year;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_events_view);

        Calendar calendar = (Calendar) getIntent().getSerializableExtra("calendar");
        day = calendar.get(Calendar.DAY_OF_MONTH);
        month = calendar.get(Calendar.MONTH);
        year = calendar.get(Calendar.YEAR);

        ((TextView) findViewById(R.id.date)).setText(DateFormat.getDateInstance(DateFormat.LONG).format(calendar.getTime()));


        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));

        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        // add one millisecond: don't show events ending at midnight
        long startOfDay = calendar.getTimeInMillis() + 1;
        calendar.set(Calendar.HOUR_OF_DAY, calendar.getActualMaximum(Calendar.HOUR_OF_DAY));
        calendar.set(Calendar.MINUTE, calendar.getActualMaximum(Calendar.MINUTE));
        calendar.set(Calendar.SECOND, calendar.getActualMaximum(Calendar.SECOND));
        calendar.set(Calendar.MILLISECOND, calendar.getActualMaximum(Calendar.MILLISECOND));
        // set end timestamp to 00:00:00
        long endOfDay = calendar.getTimeInMillis();

        Bundle bundle = new Bundle();
        bundle.putLong(START_TS, startOfDay);
        bundle.putLong(END_TS, endOfDay);

        //getLoaderManager().initLoader((int) (startOfDay / 1000L), bundle, this);
        getLoaderManager().initLoader(0, bundle, this);


        // TODO: Display Date
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.events_view, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case R.id.action_add_event:
                Intent calIntent = new Intent(Intent.ACTION_INSERT);
                calIntent.setData(CalendarContract.Events.CONTENT_URI);


                Calendar now = Calendar.getInstance();
                // if not current day: send  start time
                if (now.get(Calendar.DAY_OF_MONTH) != day || now.get(Calendar.MONTH) != month || now.get(Calendar.YEAR) != year) {
                    Calendar calendar = now;
                    // first day of displayed month
                    calendar.set(year, month, 1);
                    // round to next hour
                    calendar.set(Calendar.MINUTE, 0);
                    calendar.set(Calendar.SECOND, 0);
                    calendar.add(Calendar.HOUR_OF_DAY, 1);

                    calIntent.setType("vnd.android.cursor.item/event");
                    calIntent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME,
                            calendar.getTimeInMillis());
                }

                startActivity(calIntent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle bundle) {
        String[] selectionArgs = new String[]{String.valueOf(bundle.getLong(END_TS)), String.valueOf(bundle.getLong(START_TS))};
        return new CursorLoader(this, CalendarContract.Events.CONTENT_URI, EVENTS_PROJECTION, EVENTS_SELECTION, selectionArgs, CalendarContract.Events.DTSTART + " ASC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {

        String[] eventTitles = new String[cursor.getCount()];

        int i = 0;
        while (cursor.moveToNext()) {
            eventTitles[i] = cursor.getString(1);
            ++i;
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, eventTitles);

        ((ListView) findViewById(R.id.eventsListView)).setAdapter(adapter);

    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }
}
