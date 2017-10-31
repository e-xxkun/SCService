package qdu.edu.servlet;

import org.json.JSONObject;

public class text {
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		JSONObject getjson=new JSONObject();
		getjson.put("title", "LOAD");
		JSONObject json=new JSONObject();
		json.put("USERID", "201340604006");
		json.put("PASSWD", "123456");
		getjson.put("loadinfo", json);
//		getjson.put("STUNUM", "201440603020");
//		getjson.put("COURSENUM","DZGCCJR02");
		System.out.println(getjson.toString());
		JsonTools jt=new JsonTools(getjson.toString());
		System.out.println(jt.getJsonStr());
	}
}