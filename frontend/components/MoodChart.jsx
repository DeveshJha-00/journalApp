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
import { format, parseISO } from "date-fns";

function CustomTooltip({ active, payload, label }) {
  if (!active || !payload?.length) return null;

  return (
    <div className="bg-white border rounded-lg shadow-md p-3">
      <p className="font-medium text-sm mb-1">
        {format(parseISO(label), "MMM d, yyyy")}
      </p>
      <p className="text-orange-600 text-xs">
        Average Mood: {payload[0]?.value?.toFixed(1)}
      </p>
      <p className="text-blue-600 text-xs">
        Entries: {payload[1]?.value}
      </p>
    </div>
  );
}

export default function MoodChart({ data }) {
  return (
    <ResponsiveContainer width="100%" height={300}>
      <LineChart
        data={data}
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
          dot={{ r: 3 }}
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