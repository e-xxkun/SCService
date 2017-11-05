package qdu.edu.servlet;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JsonTools {

	private JSONObject getjson;
	private JSONObject sendjson;
	private Connection connection;
	private PreparedStatement statement;
	private String sql;
	
	private static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";  
    private static final String DB_URL = "jdbc:mysql://localhost:3306/choicedb";
    private static final String USEID = "root";
    private static final String PASSWD = "xkk0512";

	public JsonTools(String js){
		try {
			sendjson=new JSONObject();
			getjson=new JSONObject(js);
            connection();
            analyzeJson();
            connection.close();
            System.out.println("断开数据库连接");
        } catch (JSONException e) {
        	sendjson.put("get",false);
        	System.out.println("JSON字符串格式错误");
        } catch (SQLException e) {
        	sendjson.put("get",false);
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			sendjson.put("get",false);
			System.out.println("tomcat下找不到MYSQL JAR包");
		}
	}
	
	public void analyzeJson() throws SQLException{
		String title=getjson.getString("title");
		if(title.equals("LOAD"))analyzeLoad();
		else if(title.equals("GET"))analyzeGet();
		else if(title.equals("REMOVE"))analyzeRemove();
		else if(title.equals("COURSEINFO"))analyzeCInfo();
		else if(title.equals("STUINFO"))analyzeSInfo();
		else if(title.equals("STUSELECT"))analyzeSSelect();
		else if(title.equals("TEASELECT"))analyzeTSelect();
		else if(title.equals("LOADINFO"))analyzeLInfo();
		else if(title.equals("EDITINFO"))editLInfo();
		else if(title.equals("EDITPASSWD"))editPasswd();
		else System.out.println("收到未知指令");
	}
	
	private void editPasswd() throws SQLException {
		sendjson.put("title", "EDIT");
		String user=getjson.getString("user");
		String passwd=getjson.getString("PASSWD");
		String userid=getjson.getString("USERID");
		if(user.equals("STUDENT")){
			sql="update student set password='"+passwd+"' where no='"+userid+"'";
		}else sql="update teacher set password='"+passwd+"' where no='"+userid+"'";
		statement=connection.prepareStatement(sql);
		int num = statement.executeUpdate();
		if(num<=0)sendjson.put("STATUS", false);
		else sendjson.put("STATUS", true);
		sendjson.put("get",true);
	}

	private void editLInfo() throws SQLException {
		sendjson.put("title", "EDIT");
		String userid=getjson.getString("USERID");
		String phone=getjson.getString("PHONE");
		sql="update teacher set tel='"+phone+"' where no='"+userid+"'";
		statement=connection.prepareStatement(sql);
		int num = statement.executeUpdate();
		if(num<=0){
			sendjson.put("STATUS", false);
		}else sendjson.put("STATUS", true);
		sendjson.put("get",true);
	}

	private void analyzeLInfo() throws SQLException {
		sendjson.put("title", "LOADINFO");
		String user=getjson.getString("user");
		String userid=getjson.getString("USERID");
		if(user.equals("STUDENT")){
			sendjson.put("USER", "STUDENT");
			sql="select classname from a_allstudentinfo where stunum='"+userid+"'";
			statement=connection.prepareStatement(sql);
			ResultSet rs = statement.executeQuery();
			while(rs.next()){
				String classname=rs.getString("classname");
				sendjson.put("DEPART", classname);
			}
		}else{
			sendjson.put("USER", "TEACHER");
			sql="select name,phone,department,titlesum from a_allteacherinfo where teanum='"+userid+"'";
			statement=connection.prepareStatement(sql);
			ResultSet rs = statement.executeQuery();
			while(rs.next()){
				String name=rs.getString("name");
				String depart=rs.getString("department");
				int titlesum=rs.getInt("titlesum");
				String phone=rs.getString("phone");
				sendjson.put("STUNAME", name);
				sendjson.put("DEPART", depart);
				sendjson.put("TITLESUM", titlesum);
				sendjson.put("PHONE", phone+"");
			}
		}
		sendjson.put("get",true);
	}

	private void analyzeTSelect() throws SQLException {
		String coursenum=getjson.getString("COURSENUM");
		String stunum=getjson.getString("STUNUM");
		sql="update project set student_no='"+stunum+"',isyd=1 where no='"+coursenum+"'";
		statement=connection.prepareStatement(sql);
		int num = statement.executeUpdate();
		if(num<=0){
			sendjson.put("STATUS", false);
			return;
		}
		System.out.println("ssdss");
		sql="delete from pre_choice where project_no = '"+coursenum+"' and student_no != '"+stunum+"'";
		statement=connection.prepareStatement(sql);
		num = statement.executeUpdate();
		sql="delete from pre_choice where project_no != '"+coursenum+"' and student_no = '"+stunum+"'";
		statement=connection.prepareStatement(sql);
		num = statement.executeUpdate();
		sendjson.put("STATUS", true);
		analyzeGet();
	}

	private void analyzeSInfo() throws SQLException {
		sendjson.put("title", "STUINFO");
		sql="select * from a_allstudentinfo where stunum='"+getjson.getString("STUNUM")+"'";
		statement=connection.prepareStatement(sql);
		ResultSet rs = statement.executeQuery();
		JSONObject stuinfo=new JSONObject();
		while(rs.next()){
			stuinfo.put("DEPARTMANT", rs.getString("classname")+"");
			JSONArray coursearray=new JSONArray();
			sql="select * from pre_choice where student_no='"+getjson.getString("STUNUM")+"'";
			statement=connection.prepareStatement(sql);
			ResultSet srs = statement.executeQuery();
			while(srs.next()){
				coursearray.put(srs.getString("project_no"));
			}
			stuinfo.put("COURSEINFO", coursearray);
		}
		sendjson.put("STUINFO", stuinfo);
		sendjson.put("get",true);
	}

	private void analyzeCInfo() throws SQLException {
		sendjson.put("title", "COURSEINFO");
		sql="select * from a_allcourseinfo where coursenum='"+getjson.getString("COURSENUM")+"'";
		statement=connection.prepareStatement(sql);
		ResultSet rs = statement.executeQuery();
		JSONObject courseinfo=new JSONObject();
		while(rs.next()){
			courseinfo.put("TEANUM", rs.getString("teanum")+"");
			courseinfo.put("PHONE", rs.getString("phone")+"");
			courseinfo.put("DEPARTMENT", rs.getString("department")+"");
			JSONArray stuarray=new JSONArray();
			sql="select * from pre_choice where project_no='"+getjson.getString("COURSENUM")+"'";
			statement=connection.prepareStatement(sql);
			ResultSet srs = statement.executeQuery();
			while(srs.next()){
				stuarray.put(srs.getString("student_no"));
			}
			courseinfo.put("STUINFO", stuarray);
		}
		sendjson.put("COURSEINFO", courseinfo);
		sendjson.put("get",true);
	}

	private void analyzeSSelect() throws SQLException {
		String stunum=getjson.getString("STUNUM");
		JSONArray coursejson=getjson.getJSONArray("COURSENUM");
		int i=0;
		while(!coursejson.isNull(i)){
			sql="insert into pre_choice(project_no,student_no) values ('"+coursejson.getString(i)+"','"+stunum+"')";
			statement=connection.prepareStatement(sql);
			int num = statement.executeUpdate();
			if(num<=0){
				sendjson.put("STATUS", false);
				return;
			}
			i++;
		}
		sendjson.put("STATUS", true);
		analyzeGet();
	}

	private void analyzeRemove() throws SQLException{
		JSONArray coursejson=getjson.getJSONArray("COURSENUM");
		String stunum=getjson.getString("STUNUM");
		int i=0;
		while(!coursejson.isNull(i)){
			sql="delete from pre_choice where project_no='"+coursejson.getString(i)+"' and student_no='"+stunum+"'";
			statement=connection.prepareStatement(sql);
			int num = statement.executeUpdate();
			if(num<=0){
				sendjson.put("STATUS", false);
				return;
			}
			i++;
		}
		sendjson.put("STATUS", true);
		analyzeGet();
	}

	private void analyzeGet() throws SQLException {
		sendjson.put("title", "INFO");
		sql="select coursenum,title,teaname,count,stunum from a_allcourseinfo";
		statement=connection.prepareStatement(sql);
		ResultSet rs = statement.executeQuery();
		JSONArray coursearray=new JSONArray();
		while(rs.next()){
			JSONObject courseinfo=new JSONObject();
			courseinfo.put("COURSENUM", rs.getString("coursenum"));
			courseinfo.put("TITLE", rs.getString("title"));
			courseinfo.put("TEACHRE", rs.getString("teaname"));
			courseinfo.put("COUNT", rs.getString("count"));
			courseinfo.put("STUNUM", rs.getString("stunum")+"");
			coursearray.put(courseinfo);
		}
		sql="select stunum,stuname,count,suretitle from a_allstudentinfo";
		statement=connection.prepareStatement(sql);
		rs = statement.executeQuery();
		JSONArray studentarray=new JSONArray();
		while(rs.next()){
			JSONObject studentinfo=new JSONObject();
			studentinfo.put("STUNUM", rs.getString("stunum"));
			studentinfo.put("STUNAME", rs.getString("stuname"));
			studentinfo.put("COUNT", rs.getString("count"));
			studentinfo.put("TITLE", rs.getString("suretitle")+"");
			studentarray.put(studentinfo);
		}
		if(getjson.getString("user").equals("STUDENT"))stuHSCourse();
		else teaHSCourse();
		sendjson.put("ALLCOURSE", coursearray);
		sendjson.put("ALLSTUDENT", studentarray);
		sendjson.put("get", true);
	}
	
	private void teaHSCourse() throws SQLException {
		sql="select * from project where teacher_no='"+getjson.getString("TEANUM")+"'";
		statement=connection.prepareStatement(sql);
		ResultSet rs = statement.executeQuery();
		JSONArray coursearray=new JSONArray();
		while(rs.next()){
			JSONObject courseinfo=new JSONObject();
			courseinfo.put("TITLE", rs.getString("title")+"");
			courseinfo.put("COURSENUM", rs.getString("no")+"");
			courseinfo.put("ISSURE", rs.getInt("isyd")>0);
			JSONArray stuarray=new JSONArray();
			sql="select * from pre_choice where project_no='"+rs.getString("no")+"'";
			statement=connection.prepareStatement(sql);
			ResultSet srs = statement.executeQuery();
			while(srs.next()){
				stuarray.put(srs.getString("student_no"));
			}
			courseinfo.put("STUNUM", stuarray);
			coursearray.put(courseinfo);
		}
		sendjson.put("HSCOURSE", coursearray);
		sendjson.put("USER", "TEACHER");
	}

	private void stuHSCourse() throws SQLException {
		sql="select c.project_no from pre_choice as c where c.student_no='"+getjson.getString("STUNUM")+"'";
		statement=connection.prepareStatement(sql);
		ResultSet rs = statement.executeQuery();
		JSONArray hscoursearray=new JSONArray();
		while(rs.next()){
			hscoursearray.put(rs.getString("c.project_no"));
		}
		sendjson.put("HSCOURSE", hscoursearray);
		sendjson.put("USER", "STUDENT");
	}
	
	private void analyzeLoad() throws SQLException {
		sendjson.put("title", "LOAD");
		if(getjson.getString("user").equals("STUDENT"))stuLoad();
		else teaLoad();
	}
	
	private void teaLoad() throws SQLException {
		JSONObject json=getjson.getJSONObject("loadinfo");
		sql="select no,password,name from teacher where no='"+json.getString("USERID")+"' and password='"+json.getString("PASSWD")+"'";
		statement=connection.prepareStatement(sql);
		ResultSet rs = statement.executeQuery();
		if(rs.next()){
			sendjson.put("NAME",rs.getString("name"));
			sendjson.put("ISSURE", false);
			sendjson.put("get", true);
		}else {
			sendjson.put("get", false);
		}
	}

	private void stuLoad() throws SQLException {
		JSONObject json=getjson.getJSONObject("loadinfo");
		sql="select no,password from student where no='"+json.getString("USERID")+"' and password='"+json.getString("PASSWD")+"'";
		statement=connection.prepareStatement(sql);
		ResultSet rs = statement.executeQuery();
		if(rs.next()){
			sql="select stuname,classname,count,coursenum from a_allstudentinfo where stunum='"+json.getString("USERID")+"'";
			statement=connection.prepareStatement(sql);
			rs=statement.executeQuery();
			if(rs.next()){
				sendjson.put("get", true);
				sendjson.put("NAME", rs.getString("stuname"));
				sendjson.put("ISSURE", rs.getString("coursenum")==null);
			}else{
				sendjson.put("get", false);
			}
		}else {
			sendjson.put("get", false);
		}
		
	}

	public String getJsonStr(){
		return sendjson.toString();
	}
	
	private void connection() throws ClassNotFoundException, SQLException{
		Class.forName(JDBC_DRIVER);
		connection = DriverManager.getConnection(DB_URL,USEID,PASSWD);
		if(connection.isClosed())System.out.println("数据库连接失败");
		else System.out.println("数据库连接成功");
	}
	
}
