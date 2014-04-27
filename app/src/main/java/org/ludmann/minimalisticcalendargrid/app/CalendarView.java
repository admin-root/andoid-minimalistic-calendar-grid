package org.ludmann.minimalisticcalendargrid.app;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.ActionBarActivity;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.Calendar;


/**
 * Month calendar view. Main Activity.
 *
 * @author Cornelius Ludmann
 */
public class CalendarView extends ActionBarActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    /**
     * The event fields to query.
     */
    private static final String[] EVENTS_PROJECTION = new String[]{
            CalendarContract.Instances._ID,                // 0
            CalendarContract.Instances.TITLE,              // 1
            CalendarContract.Instances.BEGIN,              // 2
            CalendarContract.Instances.END,                // 3
            CalendarContract.Instances.ALL_DAY,            // 4
            CalendarContract.Instances.VISIBLE,            // 5
            CalendarContract.Instances.DISPLAY_COLOR       // 6
    };
    /**
     * The selection string for the events.
     */
    private static final String EVENTS_SELECTION =
            CalendarContract.Instances.VISIBLE + "=1";

    /**
     * ID for the loader that reads the events.
     */
    private static final int EVENTS_LOADER_ID = 0;

    /**
     * start timestamp key for bundle
     */
    private static final String START_TS = "startTS";
    /**
     * end timestamp key for bundle
     */
    private static final String END_TS = "endTS";


    /**
     * Current date.
     */
    private Calendar now;
    /**
     * The month to display.
     */
    private int month;
    /**
     * The year to display.
     */
    private int year;
    /**
     * The highest number of events in this month. (needed for a hack to equalize the row heights)
     */
    private int maxNoEvents = 0;
    /**
     * For detecting the swipe gesture.
     */
    private GestureDetectorCompat gestureDetectorCompat;


    // SET METHODS FOR MONTH AND YEAR

    /**
     * To change the view to an other month use this method to set the new month (when the year doesn't change).
     *
     * @param month The new month.
     */
    public void setMonth(int month) {
        // don't update if nothing is changed
        if (this.month != month) {
            this.month = month;
            update();
        }
    }

    /**
     * Decreases the month by one (previous month).
     */
    public void decreaseMonth() {
        if (month == 0) {
            month = 11;
            --year;
        } else {
            --month;
        }
        update();
    }

    /**
     * Increases the month by one (next month)
     */
    public void increaseMonth() {
        if (month == 11) {
            month = 0;
            ++year;
        } else {
            ++month;
        }
        update();
    }

    /**
     * To change the view to an other year use this method to set the new year (when the month doesn't change).
     *
     * @param year The new year.
     */
    public void setYear(int year) {
        // don't update if nothing is changed
        if (this.year != year) {
            this.year = year;
            update();
        }
    }

    /**
     * To change the view to an other month and year use this method to set the new month and year.
     *
     * @param month The new month.
     * @param year  The new year.
     */
    public void setMonthAndYear(int month, int year) {
        // don't update if nothing is changed
        if (this.month != month || this.year != year) {
            this.month = month;
            this.year = year;
            update();
        }
    }


    // HANDLER FOR THE ACTIVITY

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar_view);

        now = Calendar.getInstance();
        month = now.get(Calendar.MONTH);
        year = now.get(Calendar.YEAR);

        // init the month spinner
        Spinner monthSpinner = (Spinner) findViewById(R.id.month_spinner);
        ArrayAdapter<String> monthAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, CalendarLabels.getMonthNames());
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        monthSpinner.setAdapter(monthAdapter);
        monthSpinner.setSelection(month);
        monthSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
                setMonth(pos);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                setMonth(Calendar.getInstance().get(Calendar.MONTH));
            }
        });

        // init weekday labels
        setWeekdayLabels();

        // Loader Manager that reads the calendar database
        Calendar firstDayOfMonth = CalendarLabels.getFirstDayOfMonth(month, year);
        Calendar lastDayOfMonth = CalendarLabels.getLastDayOfMonth(month, year);

        Bundle bundle = new Bundle();
        bundle.putLong(START_TS, firstDayOfMonth.getTimeInMillis());
        bundle.putLong(END_TS, lastDayOfMonth.getTimeInMillis());
        getLoaderManager().initLoader(EVENTS_LOADER_ID, bundle, this);


        // swipe gestures for month change
        gestureDetectorCompat = new GestureDetectorCompat(this, new GestureListener());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.calendar_view, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case R.id.action_settings:
                return true;
            case R.id.action_today:
                setMonthAndYear(Calendar.getInstance().get(Calendar.MONTH), Calendar.getInstance().get(Calendar.YEAR));
                return true;
            case R.id.action_add_event:
                Intent calIntent = new Intent(Intent.ACTION_INSERT);
                calIntent.setData(CalendarContract.Events.CONTENT_URI);

                // if not current month: send  start time
                if (now.get(Calendar.MONTH) != month || now.get(Calendar.YEAR) != year) {
                    Calendar calendar = Calendar.getInstance();
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
            case R.id.action_reload:
                update();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    // DATA AND UI UPDATE METHODS

    /**
     * Sets the weekday labels (monday - sunday)
     */
    private void setWeekdayLabels() {
        TableRow row = (TableRow) findViewById(R.id.weekday_names_row);

        String[] weekdayNames = CalendarLabels.getWeekdayNames();

        for (int i = 1; i < row.getChildCount(); ++i) {
            TextView textView = (TextView) row.getChildAt(i);
            textView.setText(weekdayNames[i - 1]);
        }
    }

    /**
     * This method updates the view. This should be called after each update of month or year.
     */
    private void update() {

        now = Calendar.getInstance();

        // set the right month
        Spinner monthSpinner = (Spinner) findViewById(R.id.month_spinner);
        if (monthSpinner.getSelectedItemPosition() != month)
            monthSpinner.setSelection(month);

        // set the right year
        Button yearButton = (Button) findViewById(R.id.year_button);
        if (!yearButton.getText().equals(String.valueOf(year)))
            yearButton.setText(String.valueOf(year));

        // hide 'this month' button on current month
//        if (month == now.get(Calendar.MONTH) && year == now.get(Calendar.YEAR)) {
//            findViewById(id.jump_to_now).setVisibility(View.INVISIBLE);
//        } else {
//            findViewById(id.jump_to_now).setVisibility(View.VISIBLE);
//        }


        maxNoEvents = 0;

        Calendar firstDayOfMonth = CalendarLabels.getFirstDayOfMonth(month, year);
        Calendar lastDayOfMonth = CalendarLabels.getLastDayOfMonth(month, year);

        Bundle bundle = new Bundle();
        bundle.putLong(START_TS, firstDayOfMonth.getTimeInMillis());
        bundle.putLong(END_TS, lastDayOfMonth.getTimeInMillis());
        getLoaderManager().restartLoader(EVENTS_LOADER_ID, bundle, this);
    }

    /**
     * It's a hack for rows with equal height. Maybe, there is a better solution ...
     *
     * @param viewId
     */
    private void setDummyEvents(int viewId) {
        TableRow row = (TableRow) findViewById(viewId);
        if (row.getVisibility() == View.GONE)
            return;

        LayoutInflater inflater = getLayoutInflater();

        final ViewGroup cellViewGroup = (ViewGroup) row.getChildAt(0);
        //ViewGroup eventsViewGroup = (ViewGroup) ((ViewGroup) cellViewGroup.getChildAt(1)).getChildAt(0);
        ViewGroup eventsViewGroup = cellViewGroup;

        for (int i = eventsViewGroup.getChildCount() + 1; i <= maxNoEvents; ++i) {
            TextView textView = (TextView) inflater.inflate(R.layout.event, null);
            textView.setText("");
            textView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dayClick(cellViewGroup);
                }
            });
            eventsViewGroup.addView(textView);
        }
    }

    /**
     * Removes all events in a row.
     *
     * @param viewId The row ID.
     */
    private void removeAllEvents(int viewId) {
        TableRow row = (TableRow) findViewById(viewId);
        if (row.getVisibility() == View.GONE)
            return;

        LayoutInflater inflater = getLayoutInflater();

        for (int childNo = 1; childNo < row.getChildCount(); ++childNo) {
            final ViewGroup cellViewGroup = (ViewGroup) row.getChildAt(childNo);
            if (cellViewGroup.getChildCount() >= 2) {
                ViewGroup eventsViewGroup = (ViewGroup) ((ViewGroup) cellViewGroup.getChildAt(1)).getChildAt(0);

                eventsViewGroup.removeAllViews();
            }
        }
    }

    /**
     * Sets day header and adds events.
     *
     * @param viewId   The row ID.
     * @param calendar First day of the month.
     * @param cursor   The cursor with all events of the month.
     */
    private void updateRow(int viewId, Calendar calendar, Cursor cursor) {
        TableRow row = (TableRow) findViewById(viewId);

        LayoutInflater inflater = getLayoutInflater();


        TextView calendarWeek = (TextView) ((ViewGroup) row.getChildAt(0)).getChildAt(0);
        calendarWeek.setText(String.valueOf(calendar.get(Calendar.WEEK_OF_YEAR)));

        for (int childNo = 1; childNo < row.getChildCount(); ++childNo) {
            // This view group is the container of the cell that holds the day events and the number of the day of the month.
            final ViewGroup cellViewGroup = (ViewGroup) row.getChildAt(childNo);
            if (cellViewGroup.getChildCount() > 2)
                throw new IllegalStateException("cellViewGroup has more than 2 children");

            // set day of month number
            TextView textView = (TextView) cellViewGroup.getChildAt(0);
            textView.setText(String.valueOf(calendar.get(Calendar.DAY_OF_MONTH)));
            if (now.get(Calendar.YEAR) == year && now.get(Calendar.MONTH) == month && now.get(Calendar.DAY_OF_MONTH) == calendar.get(Calendar.DAY_OF_MONTH)) {
                textView.setTextAppearance(this, R.style.EventEntry_DayNumber);
                //textView.setBackgroundColor(0xffB82500);
                textView.setBackground(getResources().getDrawable(R.drawable.daynumber_now));
            } else if (calendar.get(Calendar.MONTH) != month) {
                textView.setTextAppearance(this, R.style.EventEntry_DayNumber_LastNextMonth);
                //textView.setBackgroundColor(0xFFCAE4F2);
                textView.setBackground(getResources().getDrawable(R.drawable.daynumber_lastnextweek));
            } else if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                textView.setTextAppearance(this, R.style.EventEntry_DayNumber);
                //textView.setBackgroundColor(0xff00007A);
                textView.setBackground(getResources().getDrawable(R.drawable.daynumber_weekend));
                textView.setTextColor(0xffffffff);
            } else {
                textView.setTextAppearance(this, R.style.EventEntry_DayNumber);
                //textView.setBackgroundColor(0xff4fa5d5);
                textView.setBackground(getResources().getDrawable(R.drawable.daynumber));
            }

            textView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dayClick(cellViewGroup);
                }
            });


            // add events of day

            // if there is a view group for the events: reuse it
            ViewGroup eventsViewGroup = null;
            if (cellViewGroup.getChildCount() == 2) {
                eventsViewGroup = (ViewGroup) ((ViewGroup) cellViewGroup.getChildAt(1)).getChildAt(0);
            } else {
                ViewGroup vg = new ScrollView(this);
                cellViewGroup.addView(vg);
                eventsViewGroup = new LinearLayout(this);
                ((LinearLayout) eventsViewGroup).setOrientation(LinearLayout.VERTICAL);
                vg.addView(eventsViewGroup);
            }

            eventsViewGroup.removeAllViews();

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

            int eventsToday = 0;

            //System.out.println("cursor count " + cursor.getCount());
            //System.out.println("cursor position 1: " + cursor.getPosition());

            cursor.moveToPosition(-1);
            while (cursor.moveToNext()) {
                //System.out.println("cursor position 2: " + cursor.getPosition());
                long dtstart = cursor.getLong(2);
                long dtend = cursor.getLong(3);

                if (dtstart <= endOfDay && dtend >= startOfDay) {
                    String title = cursor.getString(1);
                    boolean isAllDay = cursor.getInt(4) == 1 ? true : false;
                    int displayColor = cursor.getInt(6);

                    if (isAllDay) {
                        textView = (TextView) inflater.inflate(R.layout.event_all_day, null);
                    } else {
                        textView = (TextView) inflater.inflate(R.layout.event, null);
                    }

                    textView.setText(title);
                    textView.setTextColor(displayColor);
                    textView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            dayClick(cellViewGroup);
                        }
                    });
                    eventsViewGroup.addView(textView);
                    ++eventsToday;
                }
            }

            maxNoEvents = Math.max(maxNoEvents, eventsToday);

            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
    }


    // BUTTON HANDLER


    /**
     * Changes the view to the current month (invoked by a button).
     *
     * @param view The button that has invoked this method.
     */
    public void jumpToNow(View view) {
        setMonthAndYear(Calendar.getInstance().get(Calendar.MONTH), Calendar.getInstance().get(Calendar.YEAR));
    }

    /**
     * Opens the year picker dialog.
     *
     * @param view The button that has invoked this method.
     */
    public void pickYear(View view) {
        DialogFragment newFragment = YearPickerDialogFragment.newInstance(year);
        newFragment.show(getFragmentManager(), "year_picker");
    }

    public void dayClick(View view) {
        Intent intent = new Intent(this, EventsView.class);

        Calendar calendar = Calendar.getInstance();

        ViewGroup viewGroup = (ViewGroup) view;
        // TextView that holds the day
        TextView textView = (TextView) viewGroup.getChildAt(0);
        // TODO: How to handle last and next month?

        int day = Integer.parseInt(textView.getText().toString());
        calendar.set(year, month, day);
        intent.putExtra("calendar", calendar);

        startActivity(intent);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        this.gestureDetectorCompat.onTouchEvent(event);
        return super.onTouchEvent(event);
    }


    // GESTURE HANDLER


    /**
     * user has swiped right
     */
    private void onSwipeRight() {
        decreaseMonth();
    }

    /**
     * user has swiped left
     */
    private void onSwipeLeft() {
        increaseMonth();
    }

    /**
     * user has swiped top
     */
    private void onSwipeTop() {
    }

    /**
     * user has swiped bottom
     */
    private void onSwipeBottom() {
    }


    // LOADER MANAGER HANDLER

    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle bundle) {
        // System.out.println("CREATE LOADER");
        switch (loaderId) {
            case EVENTS_LOADER_ID:
                findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
                //ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);

                removeAllEvents(R.id.row_1);
                removeAllEvents(R.id.row_2);
                removeAllEvents(R.id.row_3);
                removeAllEvents(R.id.row_4);
                removeAllEvents(R.id.row_5);
                removeAllEvents(R.id.row_6);

                Uri.Builder builder = CalendarContract.Instances.CONTENT_URI.buildUpon();
                ContentUris.appendId(builder, bundle.getLong(START_TS));
                ContentUris.appendId(builder, bundle.getLong(END_TS));
                return new CursorLoader(this, builder.build(), EVENTS_PROJECTION, EVENTS_SELECTION, null, CalendarContract.Instances.BEGIN + " ASC");
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

        //System.out.println("LOAD FINISH");

        switch (loader.getId()) {

            case EVENTS_LOADER_ID:
                Calendar calendar = CalendarLabels.getFirstDayOfMonth(month, year);


                updateRow(R.id.row_1, calendar, cursor);
                updateRow(R.id.row_2, calendar, cursor);
                updateRow(R.id.row_3, calendar, cursor);
                updateRow(R.id.row_4, calendar, cursor);
                if (calendar.get(Calendar.MONTH) != month) {
                    findViewById(R.id.row_5).setVisibility(View.GONE);
                } else {
                    findViewById(R.id.row_5).setVisibility(View.VISIBLE);
                    updateRow(R.id.row_5, calendar, cursor);
                }
                if (calendar.get(Calendar.MONTH) != month) {
                    findViewById(R.id.row_6).setVisibility(View.GONE);
                } else {
                    findViewById(R.id.row_6).setVisibility(View.VISIBLE);
                    updateRow(R.id.row_6, calendar, cursor);
                }

                setDummyEvents(R.id.row_1);
                setDummyEvents(R.id.row_2);
                setDummyEvents(R.id.row_3);
                setDummyEvents(R.id.row_4);
                setDummyEvents(R.id.row_5);
                setDummyEvents(R.id.row_6);
                findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
                return;
        }


    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        //System.out.println("RESET LOADER");
        switch (loader.getId()) {

            case EVENTS_LOADER_ID:
                findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
                //ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);

                removeAllEvents(R.id.row_1);
                removeAllEvents(R.id.row_2);
                removeAllEvents(R.id.row_3);
                removeAllEvents(R.id.row_4);
                removeAllEvents(R.id.row_5);
                removeAllEvents(R.id.row_6);
                return;
        }
    }


    // CLASSES

    /**
     * Picks the year.
     */
    private final static class YearPickerDialogFragment extends DialogFragment {

        private static final String KEY_SELECTED_YEAR = "selectedYear";

        public static YearPickerDialogFragment newInstance() {
            YearPickerDialogFragment frag = new YearPickerDialogFragment();
            Bundle args = new Bundle();
            args.putInt("selectedYear", Calendar.getInstance().get(Calendar.YEAR));
            frag.setArguments(args);
            return frag;
        }

        public static YearPickerDialogFragment newInstance(int selectedYear) {
            YearPickerDialogFragment frag = new YearPickerDialogFragment();
            Bundle args = new Bundle();
            args.putInt(KEY_SELECTED_YEAR, selectedYear);
            frag.setArguments(args);
            return frag;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View view = inflater.inflate(R.layout.year_picker, null);
            final NumberPicker picker = (NumberPicker) view.findViewById(R.id.year_picker);
            picker.setMinValue(Calendar.getInstance().getMinimum(Calendar.YEAR));
            picker.setMaxValue(Calendar.getInstance().getMaximum(Calendar.YEAR));
            picker.setValue(getArguments().getInt(KEY_SELECTED_YEAR));
            builder.setTitle(getString(R.string.pick_year))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int id) {
                                    ((CalendarView) getActivity()).setYear(picker.getValue());
                                }
                            }
                    )
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                }
                            }
                    )
                    .setView(view)
            ;
            return builder.create();
        }
    }

    /**
     * Detects swipe gestures.
     * <p/>
     * Source: http://stackoverflow.com/questions/4139288/android-how-to-handle-right-to-left-swipe-gestures
     */
    private final class GestureListener extends GestureDetector.SimpleOnGestureListener {

        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;


        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            boolean result = false;
            try {
                float diffY = e2.getY() - e1.getY();
                float diffX = e2.getX() - e1.getX();
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            onSwipeRight();
                        } else {
                            onSwipeLeft();
                        }
                    }
                } else {
                    if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffY > 0) {
                            onSwipeBottom();
                        } else {
                            onSwipeTop();
                        }
                    }
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
            return result;
        }
    }
}