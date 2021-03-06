package com.evancharlton.mileage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Set;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Message;

public class SQLExporter implements Runnable {
	private Handler m_handler;

	public SQLExporter(Handler handler) {
		m_handler = handler;
	}

	public void run() {
		HashMap<String, String> fillupsProjection = FillUpsProvider.getFillUpsProjection();
		HashMap<String, String> vehiclesProjection = FillUpsProvider.getVehiclesProjection();

		Set<String> keySet = fillupsProjection.keySet();
		String[] proj = keySet.toArray(new String[keySet.size()]);
		SQLiteDatabase db = SQLiteDatabase.openDatabase("/data/data/" + Mileage.PACKAGE + "/databases/" + FillUpsProvider.DATABASE_NAME, null, SQLiteDatabase.OPEN_READONLY);
		Cursor c = db.query(FillUpsProvider.FILLUPS_TABLE_NAME, proj, null, null, null, null, FillUps._ID + " ASC");

		StringBuilder sb = new StringBuilder();
		sb.append("-- Exported database: ").append(FillUpsProvider.DATABASE_NAME).append("\n");
		sb.append("-- Exported version: ").append(FillUpsProvider.DATABASE_VERSION).append("\n");
		sb.append("-- Begin table: ").append(FillUpsProvider.FILLUPS_TABLE_NAME).append("\n");
		c.moveToFirst();
		while (c.isAfterLast() == false) {
			sb.append("INSERT INTO ").append(FillUpsProvider.FILLUPS_TABLE_NAME).append(" ");
			keySetToSQL(keySet, sb);
			keySetToValues(keySet, sb, c);
			c.moveToNext();
		}
		sb.append("-- End table: ").append(FillUpsProvider.FILLUPS_TABLE_NAME).append("\n");

		sb.append("-- Begin table: ").append(FillUpsProvider.VEHICLES_TABLE_NAME).append("\n");
		keySet = vehiclesProjection.keySet();
		proj = keySet.toArray(new String[keySet.size()]);
		c = db.query(FillUpsProvider.VEHICLES_TABLE_NAME, proj, null, null, null, null, Vehicles._ID + " ASC");
		c.moveToFirst();
		while (c.isAfterLast() == false) {
			sb.append("INSERT INTO ").append(FillUpsProvider.VEHICLES_TABLE_NAME);
			keySetToSQL(keySet, sb);
			keySetToValues(keySet, sb, c);
			c.moveToNext();
		}
		sb.append("-- End table: ").append(FillUpsProvider.VEHICLES_TABLE_NAME).append("\n");

		db.close();

		// write to a file
		try {
			File output = new File("/sdcard/mileage.sql");
			FileWriter out = new FileWriter(output);

			out.write(sb.toString());
			out.flush();
			out.close();
		} catch (final IOException e) {
			m_handler.post(new Runnable() {
				public void run() {
					Message msg = new Message();
					msg.what = 0;
					msg.obj = e.getMessage();
					msg.arg2 = R.string.error_exporting_data;
					m_handler.handleMessage(msg);
				}
			});
			return;
		}

		m_handler.post(new Runnable() {
			public void run() {
				Message msg = new Message();
				msg.what = 1;
				msg.arg1 = R.string.export_finished_msg;
				msg.arg2 = R.string.export_finished;
				msg.obj = "mileage.sql";
				m_handler.handleMessage(msg);
			}
		});
	}

	private void keySetToSQL(Set<String> columns, StringBuilder sb) {
		sb.append(" (");
		for (String key : columns) {
			sb.append("'").append(key).append("', ");
		}
		sb.deleteCharAt(sb.length() - 1);
		sb.deleteCharAt(sb.length() - 1);
		sb.append(") ");
	}

	private void keySetToValues(Set<String> columns, StringBuilder sb, Cursor c) {
		sb.append(" VALUES (");
		int i = 1;
		for (String key : columns) {
			String val = c.getString(c.getColumnIndex(key));
			if (val == null) {
				val = "";
			}
			val = val.replaceAll("'", "\\'");
			sb.append("'").append(val).append("'");
			if (i != c.getColumnCount()) {
				sb.append(", ");
			}
			i++;
		}
		sb.append(");\n");
	}
}
