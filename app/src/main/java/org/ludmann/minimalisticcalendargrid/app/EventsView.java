package org.ludmann.minimalisticcalendargrid.app;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
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
            CalendarContract.Events.DISPLAY_COLOR,        // 8
            CalendarContract.Events.CALENDAR_DISPLAY_NAME   // 9
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

        ((ListView) findViewById(R.id.eventsListView)).setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                Event event = (Event) adapterView.getItemAtPosition(position);

                Intent calIntent = new Intent(Intent.ACTION_VIEW);
                calIntent.setData(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, event.id));
                startActivity(calIntent);
            }
        });



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

        Event[] events = new Event[cursor.getCount()];

        int i = 0;
        while (cursor.moveToNext()) {
            events[i] = new Event();
            events[i].id = cursor.getLong(0);
            events[i].title = cursor.getString(1);
            events[i].location = cursor.getString(2);
            events[i].dtstart = cursor.getLong(3);
            events[i].dtend = cursor.getLong(4);
            events[i].allDay = cursor.getInt(5) == 0 ? false : true;
            events[i].deleted = cursor.getInt(6) == 0 ? false : true;
            events[i].visible = cursor.getInt(7) == 0 ? false : true;
            events[i].displayColor = cursor.getInt(8);
            events[i].calendarDisplayName = cursor.getString(9);
            ++i;
        }

        ListAdapter adapter = new EventsAdapter(this, events);

        ((ListView) findViewById(R.id.eventsListView)).setAdapter(adapter);

    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }

    /**
     * Holds the event in the ArrayAdapter.
     * <p/>
     * To make it as simple as possible I've passed on the getters and setters.
     */
    private class Event {
        public long id;
        public String title;
        public String location;
        public long dtstart;
        public long dtend;
        public boolean allDay;
        public boolean deleted;
        public boolean visible;
        public int displayColor;
        public String calendarDisplayName;
    }


    private class EventsAdapter extends ArrayAdapter<Event> {
        public EventsAdapter(Context context, Event[] events) {
            super(context, R.layout.event_item, events);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Get the data item for this position
            Event event = getItem(position);

            // Check if an existing view is being reused, otherwise inflate the view
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.event_item, parent, false);
            }

            TextView textViewTime = (TextView) convertView.findViewById(R.id.time);
            TextView textViewTitle = (TextView) convertView.findViewById(R.id.title);
            TextView textViewLocation = (TextView) convertView.findViewById(R.id.location);
            TextView textViewExtraInfo = (TextView) convertView.findViewById(R.id.extrainfo);


            Calendar start = Calendar.getInstance();
            // dtstart is in UTC
            start.setTimeInMillis(event.dtstart - TimeZone.getDefault().getOffset(event.dtstart));
            Calendar end = Calendar.getInstance();
            // dtend is in UTC
            end.setTimeInMillis(event.dtend - TimeZone.getDefault().getOffset(event.dtend));


            Calendar startOfDay = Calendar.getInstance();
            //startOfDay.setTimeZone(TimeZone.getTimeZone("UTC"));
            startOfDay.set(year, month, day, 0, 0, 0);
            startOfDay.set(Calendar.MILLISECOND, 0);
            Calendar endOfDay = Calendar.getInstance();
            //endOfDay.setTimeZone(TimeZone.getTimeZone("UTC"));
            endOfDay.set(year, month, day, 23, 59, 59);
            endOfDay.set(Calendar.MILLISECOND, endOfDay.getActualMaximum(Calendar.MILLISECOND));
            endOfDay.add(Calendar.MILLISECOND, 1);
            boolean onlyThisDay = !start.before(startOfDay) && !end.after(endOfDay);
            //System.out.println("start before startofday? " + start.before(startOfDay) + " " + start.getTimeInMillis() + " " + startOfDay.getTimeInMillis());
            //System.out.println("end after endofday? " + end.before(endOfDay) + " " + end.getTimeInMillis() + " " + endOfDay.getTimeInMillis());

            String timeText;
            if (event.allDay) {
                if (onlyThisDay) {
                    timeText = null;
                } else {
                    DateFormat dateFormat = DateFormat.getDateInstance();
                    //DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG);

                    // Because all day events end at 00:00 of the next day ...
                    end.add(Calendar.MILLISECOND, -1);
                    timeText = dateFormat.format(start.getTime()) + " – " + dateFormat.format(end.getTime());
                }
            } else {
                DateFormat dateFormat;
                if (onlyThisDay) {
                    dateFormat = DateFormat.getTimeInstance(DateFormat.SHORT);
                } else {
                    dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);

                }
                timeText = dateFormat.format(start.getTime()) + " – " + dateFormat.format(end.getTime());
            }


            if (timeText == null) {
                textViewTime.setVisibility(View.GONE);
            } else {
                textViewTime.setText(timeText);
            }
            textViewTitle.setText(event.title);
            textViewTitle.setTextColor(event.displayColor);
            if (event.location == null || event.location.trim().length() == 0) {
                textViewLocation.setVisibility(View.GONE);
            } else {
                textViewLocation.setText(event.location);
            }

            textViewExtraInfo.setText(getString(R.string.calendar_display_name) + ": " + event.calendarDisplayName);

            return convertView;
        }
    }
}
