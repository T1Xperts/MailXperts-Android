package au.com.t1xperts.mailxperts;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** Smart Contacts autocomplete across local learned contacts and permissioned device contacts. */
final class RecipientSuggestionAdapter extends BaseAdapter implements Filterable {
    private final Context context;
    private final RecipientHistory history;
    private final ArrayList<String> items = new ArrayList<>();
    private volatile List<RecipientDirectory.Entry> deviceEntries;

    RecipientSuggestionAdapter(Context context, RecipientHistory history) {
        this.context = context.getApplicationContext();
        this.history = history;
    }

    @Override public int getCount() { return items.size(); }
    @Override public Object getItem(int position) { return items.get(position); }
    @Override public long getItemId(int position) { return position; }

    @Override public View getView(int position, View convertView, ViewGroup parent) {
        TextView text = convertView instanceof TextView
                ? (TextView) convertView : Ui.text(parent.getContext(), "");
        text.setText(items.get(position));
        text.setTextColor(Ui.textColor(parent.getContext()));
        text.setBackgroundColor(Ui.panel(parent.getContext()));
        text.setPadding(Ui.dp(parent.getContext(), 14), Ui.dp(parent.getContext(), 12),
                Ui.dp(parent.getContext(), 14), Ui.dp(parent.getContext(), 12));
        return text;
    }

    @Override public Filter getFilter() {
        return new Filter() {
            @Override protected FilterResults performFiltering(CharSequence constraint) {
                String query = constraint == null ? "" : constraint.toString();
                List<RecipientDirectory.Entry> device = deviceEntries;
                if (device == null) {
                    device = DeviceContactDirectory.load(context);
                    deviceEntries = device;
                }
                ArrayList<RecipientDirectory.Entry> allowedDevice = new ArrayList<>();
                for (RecipientDirectory.Entry entry : device) {
                    if (!history.isBlocked(entry.email)) allowedDevice.add(entry);
                }
                List<RecipientDirectory.Entry> merged = RecipientDirectory.mergeEntries(
                        history.entries(), allowedDevice);
                List<String> suggestions = RecipientDirectory.suggestions(merged, query, 10);
                FilterResults results = new FilterResults();
                results.values = suggestions;
                results.count = suggestions.size();
                return results;
            }

            @SuppressWarnings("unchecked")
            @Override protected void publishResults(CharSequence constraint, FilterResults results) {
                items.clear();
                if (results != null && results.values instanceof List) {
                    items.addAll((List<String>) results.values);
                }
                notifyDataSetChanged();
            }

            @Override public CharSequence convertResultToString(Object resultValue) {
                return resultValue == null ? "" : resultValue.toString();
            }
        };
    }
}
