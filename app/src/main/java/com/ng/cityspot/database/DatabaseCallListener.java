package com.ng.cityspot.database;

import com.ng.cityspot.database.data.Data;

public interface DatabaseCallListener {
	void onDatabaseCallRespond(DatabaseCallTask task, Data<?> data);
	void onDatabaseCallFail(DatabaseCallTask task, Exception exception);
}
