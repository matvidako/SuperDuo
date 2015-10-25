package barqsoft.footballscores.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import barqsoft.footballscores.service.MatchFetchService;

public class LatestMatchWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Toast.makeText(context, "Update!", Toast.LENGTH_SHORT).show();
        context.startService(new Intent(context, MatchFetchService.class));
    }

}
