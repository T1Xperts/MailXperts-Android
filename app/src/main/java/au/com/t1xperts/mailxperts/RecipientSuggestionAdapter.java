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

/** Substring-capable local recipient autocomplete adapter. */
final class RecipientSuggestionAdapter extends BaseAdapter implements Filterable {
    private final Context context;
    private final RecipientHistory history;
    private final ArrayList<String> items = new ArrayList<>();

    RecipientSuggestionAdapter(Context context, RecipientHistory history) {
        this.context = context;
        this.history = history;
    }

    @Override public int getCount() { return items.size(); }
    @Override public Object getItem(int position) { return items.get(position); }
    @Override public long getItemId(int position) { return position; }

    @Override public View getView(int position, View convertView, ViewGroup parent) {
        TextView text = convertView instanceof TextView
                ? (TextView) convertView : Ui.text(context, "");
        text.setText(items.get(position));
        text.setTextColor(Ui.textColor(context));
        text.setBackgroundColor(Ui.panel(context));
        text.setPadding(Ui.dp(context, 14), Ui.dp(context, 12),
                Ui.dp(context, 14), Ui.dp(context, 12));
        return text;
    }

    @Override public Filter getFilter() {
        return new Filter() {
            @Override protected FilterResults performFiltering(CharSequence constraint) {
                String query = constraint == null ? "" : constraint.toString();
                List<String> suggestions = history.suggestions(query, 10);
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
