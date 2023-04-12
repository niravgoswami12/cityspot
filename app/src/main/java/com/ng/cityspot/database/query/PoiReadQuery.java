package com.ng.cityspot.database.query;

import com.ng.cityspot.database.dao.PoiDAO;
import com.ng.cityspot.database.data.Data;
import com.ng.cityspot.database.model.PoiModel;

import java.sql.SQLException;

public class PoiReadQuery extends Query {
	private long mId;

	public PoiReadQuery(long id) {
		mId = id;
	}

	@Override
	public Data<PoiModel> processData() throws SQLException {
		Data<PoiModel> data = new Data<>();
		data.setDataObject(PoiDAO.read(mId));
		return data;
	}
}
