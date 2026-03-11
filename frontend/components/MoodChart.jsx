"use client";

import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend,
} from "recharts";
import { format, parseISO, eachDayOfInterval, subDays } from "date-fns";

function CustomTooltip({ active, payload, label }) {
  if (!active || !payload?.length) return null;

  const moodPayload = payload.find((p) => p.dataKey === "avgMood");
  const entriesPayload = payload.find((p) => p.dataKey === "entries");

  return (
    <div className="bg-white dark:bg-gray-800 border dark:border-gray-700 rounded-lg shadow-md p-3">
      <p className="font-medium text-sm mb-1 dark:text-gray-100">
        {format(parseISO(label), "MMM d, yyyy")}
      </p>
      {moodPayload?.value != null && (
        <p className="text-orange-600 dark:text-orange-400 text-xs">
          Average Mood: {moodPayload.value.toFixed(1)}
        </p>
      )}
      <p className="text-blue-600 dark:text-blue-400 text-xs">
        Entries: {entriesPayload?.value ?? 0}
      </p>
    </div>
  );
}

export default function MoodChart({ data, period = "7d" }) {
  // Fill in all dates in the selected period so lines always render
  const days = parseInt(period);
  const endDate = new Date();
  const startDate = subDays(endDate, days - 1);

  const allDates = eachDayOfInterval({ start: startDate, end: endDate });
  const dataMap = new Map(data.map((d) => [d.date, d]));

  const filledData = allDates.map((date) => {
    const key = format(date, "yyyy-MM-dd");
    const existing = dataMap.get(key);
    return existing || { date: key, avgMood: null, entries: 0 };
  });

  return (
    <ResponsiveContainer width="100%" height={300}>
      <LineChart
        data={filledData}
        margin={{ top: 5, right: 20, left: 10, bottom: 5 }}
      >
        <CartesianGrid strokeDasharray="3 3" />

        <XAxis
          dataKey="date"
          tickFormatter={(date) =>
            format(parseISO(date), "MMM d")
          }
        />

        <YAxis
          yAxisId="left"
          domain={[0, 10]}
        />

        <YAxis
          yAxisId="right"
          orientation="right"
          domain={[0, "auto"]}
          allowDecimals={false}
        />

        <Tooltip content={<CustomTooltip />} />
        <Legend />

        <Line
          yAxisId="left"
          type="monotone"
          dataKey="avgMood"
          stroke="#f97316"
          strokeWidth={2}
          dot={{ r: 4 }}
          connectNulls
          name="Average Mood"
        />

        <Line
          yAxisId="right"
          type="monotone"
          dataKey="entries"
          stroke="#3b82f6"
          strokeWidth={2}
          dot={{ r: 3 }}
          name="Number of Entries"
        />
      </LineChart>
    </ResponsiveContainer>
  );
}