package qdu.edu.servlet;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JsonTools {

	private JSONObject getjson;
	private JSONObject sendjson;
	private SqlTools sql;
	
	public JsonTools(String js){
		try {
			sql=new SqlTools();
			sendjson=new JSONObject();
			getjson=new JSONObject(js);
            analyzeJson();
            System.out.println("断开数据库连接");
        } catch (JSONException e) {
        	sendjson.put("STATUS", false);
        	System.out.println("JSON字符串格式错误");
        	e.printStackTrace();
        } catch (SQLException e) {
        	if(!sql.isCommit()){
        		sql.rollback();
        		analyzeGet();
        	}
        	sendjson.put("STATUS", false);
        	System.out.println("提交出错");
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			sendjson.put("STATUS", false);
			System.out.println("tomcat下找不到MYSQL JAR包");
			e.printStackTrace();
		}finally{
			sql.close();
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
		else if(title.equals("USERINFO"))analyzeUInfo();
		else if(title.equals("EDITINFO"))editTInfo();
		else if(title.equals("DELCOURSE"))delCourse();
		else if(title.equals("RENAME"))renameCourse();
		else if(title.equals("ADDCOURSE"))addCourse();
		else if(title.equals("EDITPASSWD"))editPasswd();
		else if(title.equals("EVALUATE"))evaluation();
		else if(title.equals("SCORE"))scoreCourse();
		else System.out.println("收到未知指令");
	}
	
	private void evaluation() throws JSONException, SQLException {
		sendjson.put("title", "EVALUATE");
		if(getjson.getString("user").equals("STUDENT"))sql.update("student","grade="+getjson.getInt("GRADE"), "no="+getjson.getString("USERID"));
		else sql.update("teacher", "grade="+getjson.getInt("GRADE"), "no="+getjson.getString("USERID"));
		sendjson.put("GRADE", getjson.getInt("GRADE"));
		sendjson.put("STATUS", true);
	}

	private void addCourse() throws SQLException {
		String teanum=getjson.getString("USERID");
		String title=getjson.getString("TITLE");
		ResultSet rs =sql.select("departcode,teacode,cur", "a_allteacherinfo", "teanum='"+teanum+"'");
		while(rs.next()){
			String teacode=rs.getString("teacode");
			String departcode=rs.getString("departcode");
			int cur=rs.getInt("cur");
			String no;
			if(cur<10)no=departcode+teacode+"0"+cur;
			else no=departcode+teacode+cur;
			sql.startCommit();
			sql.insert("project", "teacher_no,no,title", "'"+teanum+"','"+no+"','"+title+"'");
			sql.update("teacher", "title_cur=title_cur+1,title_sum=title_sum+1", "no='"+teanum+"'");
			sql.commit();
			analyzeGet();
		}
	}

	private void scoreCourse() throws SQLException {
		String coursenum=getjson.getString("COURSENUM");
		String score=getjson.getString("SCORE");
		sql.update("project", "score='"+score+"'", "no='"+coursenum+"'");
		analyzeGet();
	}
	
	private void renameCourse() throws SQLException {
		String coursenum=getjson.getString("COURSENUM");
		String title=getjson.getString("TITLE");
		sql.update("project", "title='"+title+"'", "no='"+coursenum+"'");
		analyzeGet();
	}

	private void delCourse() throws SQLException {
		String teanum=getjson.getString("USERID");
		String coursenum=getjson.getString("COURSENUM");
		sql.startCommit();
		sql.delete("project", "no='"+coursenum+"'");
		sql.update("teacher", "title_sum=title_sum-1", "no='"+teanum+"'");
		sql.commit();
		analyzeGet();
	}

	private void editPasswd() throws SQLException {
		sendjson.put("title", "EDIT");
		String user=getjson.getString("user");
		String passwd=getjson.getString("PASSWD");
		String userid=getjson.getString("USERID");
		String type;
		if(user.equals("STUDENT"))type="student";
		else type="teacher";
		sql.update(type, "password='"+passwd+"'", "no='"+userid+"'");
		sendjson.put("STATUS", true);
	}

	private void editTInfo() throws SQLException {
		sendjson.put("title", "EDIT");
		String userid=getjson.getString("USERID");
		String phone=getjson.getString("PHONE");
		sql.update("teacher", "tel='"+phone+"'", "no='"+userid+"'");
		sendjson.put("STATUS", true);
	}

	private void analyzeUInfo() throws SQLException {
		sendjson.put("title", "USERINFO");
		String user=getjson.getString("user");
		String userid=getjson.getString("USERID");
		if(user.equals("STUDENT")){
			sendjson.put("USER", "STUDENT");
			ResultSet rs = sql.select("classname", "a_allstudentinfo", "stunum='"+userid+"'");
			while(rs.next()){
				String classname=rs.getString("classname");
				sendjson.put("DEPART", classname);
			}
		}else{
			sendjson.put("USER", "TEACHER");
			ResultSet rs = sql.select("name,phone,department,titlesum", "a_allteacherinfo", "teanum='"+userid+"'");
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
		sendjson.put("STATUS", true);
	}

	private void analyzeSInfo() throws SQLException {
		sendjson.put("title", "STUINFO");
		ResultSet rs = sql.select("*", "a_allstudentinfo", "stunum='"+getjson.getString("STUNUM")+"'");
		JSONObject stuinfo=new JSONObject();
		while(rs.next()){
			stuinfo.put("DEPARTMANT", rs.getString("classname")+"");
			JSONArray coursearray=new JSONArray();
			ResultSet srs = sql.select("*", "pre_choice", "student_no='"+getjson.getString("STUNUM")+"'");
			while(srs.next()){
				coursearray.put(srs.getString("project_no"));
			}
			stuinfo.put("COURSEINFO", coursearray);
		}
		sendjson.put("STUINFO", stuinfo);
		sendjson.put("STATUS", true);
	}

	private void analyzeCInfo() throws SQLException {
		sendjson.put("title", "COURSEINFO");
		ResultSet rs = sql.select("*", "a_allcourseinfo", "coursenum='"+getjson.getString("COURSENUM")+"'");
		JSONObject courseinfo=new JSONObject();
		while(rs.next()){
			courseinfo.put("TEANUM", rs.getString("teanum")+"");
			courseinfo.put("PHONE", rs.getString("phone")+"");
			courseinfo.put("DEPARTMENT", rs.getString("department")+"");
			JSONArray stuarray=new JSONArray();
			ResultSet srs = sql.select("*", "pre_choice", "project_no='"+getjson.getString("COURSENUM")+"'");
			while(srs.next()){
				stuarray.put(srs.getString("student_no"));
			}
			courseinfo.put("STUINFO", stuarray);
		}
		sendjson.put("COURSEINFO", courseinfo);
		sendjson.put("STATUS", true);
	}
//老师选学生
	private void analyzeTSelect() throws SQLException {
		String coursenum=getjson.getString("COURSENUM");
		String stunum=getjson.getString("STUNUM");
		sql.startCommit();
		sql.update("project", "student_no='"+stunum+"',isyd=1", "no='"+coursenum+"'");
		sql.delete("pre_choice", "project_no = '"+coursenum+"' and student_no != '"+stunum+"'");
		sql.delete("pre_choice", "project_no != '"+coursenum+"' and student_no = '"+stunum+"'");
		sql.commit();
		analyzeGet();
	}
//学生选课题
	private void analyzeSSelect() throws SQLException {
		String stunum=getjson.getString("USERID");
		JSONArray coursejson=getjson.getJSONArray("COURSENUM");
		int i=0;
		sql.startCommit();
		while(!coursejson.isNull(i)){
			sql.insert("pre_choice", "project_no,student_no", "'"+coursejson.getString(i)+"','"+stunum+"'");
			i++;
		}
		sql.commit();
		analyzeGet();
	}
//学生删除课题
	private void analyzeRemove() throws SQLException{
		JSONArray coursejson=getjson.getJSONArray("COURSENUM");
		String stunum=getjson.getString("USERID");
		int i=0;
		sql.startCommit();
		while(!coursejson.isNull(i)){
			sql.delete("pre_choice", "project_no='"+coursejson.getString(i)+"' and student_no='"+stunum+"'");
			i++;
		}
		sql.commit();
		analyzeGet();
	}

	private void analyzeGet(){
		sendjson.put("title", "INFO");
		ResultSet rs;
		JSONArray coursearray=new JSONArray();
		JSONArray studentarray=new JSONArray();
		try {
			rs = sql.select("coursenum,title,teaname,count,stunum", "a_allcourseinfo");
			while(rs.next()){
				JSONObject courseinfo=new JSONObject();
				courseinfo.put("COURSENUM", rs.getString("coursenum"));
				courseinfo.put("TITLE", rs.getString("title"));
				courseinfo.put("TEACHRE", rs.getString("teaname"));
				courseinfo.put("COUNT", rs.getString("count"));
				courseinfo.put("STUNUM", rs.getString("stunum")+"");
				coursearray.put(courseinfo);
			}
			rs = sql.select("stunum,stuname,count,coursenum", "a_allstudentinfo");
			while(rs.next()){
				JSONObject studentinfo=new JSONObject();
				studentinfo.put("STUNUM", rs.getString("stunum"));
				studentinfo.put("STUNAME", rs.getString("stuname"));
				studentinfo.put("COUNT", rs.getString("count"));
				studentinfo.put("COURSENUM", rs.getString("coursenum")+"");
				studentarray.put(studentinfo);
			}
			if(getjson.getString("user").equals("STUDENT"))stuHSCourse();
			else teaHSCourse();
		} catch (SQLException e) {
			sendjson.put("STATUS", false);
			e.printStackTrace();
		}
		sendjson.put("ALLCOURSE", coursearray);
		sendjson.put("ALLSTUDENT", studentarray);
		sendjson.put("STATUS", true);
	}
	
	private void teaHSCourse() throws SQLException {
		ResultSet rs = sql.select("*", "project", "teacher_no='"+getjson.getString("USERID")+"'");
		JSONArray coursearray=new JSONArray();
		while(rs.next()){
			JSONObject courseinfo=new JSONObject();
			courseinfo.put("TITLE", rs.getString("title")+"");
			courseinfo.put("COURSENUM", rs.getString("no")+"");
			courseinfo.put("SCORE", rs.getString("score")+"");
			courseinfo.put("ISSURE", rs.getInt("isyd")>0);
			JSONArray stuarray=new JSONArray();
			ResultSet srs = sql.select("*", "pre_choice", "project_no='"+rs.getString("no")+"'");
			while(srs.next()){
				stuarray.put(srs.getString("student_no"));
			}
			courseinfo.put("STUNUM", stuarray);
			coursearray.put(courseinfo);
		}
		rs = sql.select("title_max,title_sum", "teacher", "no='"+getjson.getString("USERID")+"'");
		if(rs.next()){
			sendjson.put("COURSEMAX",rs.getInt("title_max"));
			sendjson.put("COURSESUM",rs.getInt("title_sum"));
		}
		sendjson.put("HSCOURSE", coursearray);
		sendjson.put("USER", "TEACHER");
	}

	private void stuHSCourse() throws SQLException {
		ResultSet rs = sql.select("project_no", "pre_choice", "student_no='"+getjson.getString("USERID")+"'");
		JSONArray hscoursearray=new JSONArray();
		while(rs.next()){
			hscoursearray.put(rs.getString("project_no"));
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
		int departnum;
		ResultSet rs = sql.select("no,password,name,depart_no,title_max,title_sum,grade", "teacher", "no='"+getjson.getString("USERID")+"' and password='"+getjson.getString("PASSWD")+"'");
		if(rs.next()){
			departnum=rs.getInt("depart_no");
			SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date now =new Date();
			sql.insert("mylog","flag,no,name,time,type", "'0','"+getjson.getString("USERID")+"','"+rs.getString("name")+"','"+format.format(now)+"','21'");
			sendjson.put("COURSEMAX",rs.getInt("title_max"));
			sendjson.put("COURSESUM",rs.getInt("title_sum"));
			sendjson.put("NAME",rs.getString("name"));
			sendjson.put("DEPARTNUM", departnum);
			sendjson.put("GRADE",rs.getInt("grade"));
			sendjson.put("ISSURE", false);
			rs = sql.select("*", "flag_date","depart_no="+departnum);
			if(rs.next()){
				String time="",title="";
				try {
					if(now.compareTo(format.parse(time=rs.getString("tea_title_begin_date")))<0)title="TITLEBEGIN";
					else if(now.compareTo(format.parse(time=rs.getString("tea_title_end_date")))<0)title="TITLEEND";
					else if(now.compareTo(format.parse(time=rs.getString("tea_choice_begin_date")))<0)title="CHOICEBEGIN";
					else if(now.compareTo(format.parse(time=rs.getString("tea_choice_end_date")))<0)title="CHOICEEND";
					else if(now.compareTo(format.parse(time=rs.getString("tea_score_begin_date")))<0)title="SCOREBEGIN";
					else if(now.compareTo(format.parse(time=rs.getString("tea_score_end_date")))<0)title="SCOREEND";
					else {
						time="0";
						title="END";
					}
					sendjson.put("TIMETITLE", title);
					sendjson.put("TIME", time);
				} catch (ParseException e) {
					System.out.println("时间格式错误");
				}
			}else {
				sendjson.put("STATUS", false);
				return;
			}
			sendjson.put("STATUS", true);
		}else sendjson.put("STATUS", false);
	}

	private void stuLoad() throws SQLException {
		int departnum;
		ResultSet rs = sql.select("no,password,name,depart_no,grade", "student", "no='"+getjson.getString("USERID")+"' and password='"+getjson.getString("PASSWD")+"'");
		if(rs.next()){
			sendjson.put("GRADE", rs.getInt("grade"));
			departnum=rs.getInt("depart_no");
			sendjson.put("DEPARTNUM", departnum);
			SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date now =new Date();
			sql.insert("mylog","flag,no,name,time,type","'1','"+getjson.getString("USERID")+"','"+rs.getString("name")+"','"+format.format(now)+"','21'");
			rs=sql.select("stuname,issure,coursenum", "a_allstudentinfo", "stunum='"+getjson.getString("USERID")+"'");
			if(rs.next()){
				sendjson.put("NAME", rs.getString("stuname"));
				sendjson.put("ISSURE",rs.getInt("issure")==1);
				if(rs.getInt("issure")==1){
					String coursenum=rs.getString("coursenum");
					rs = sql.select("*", "a_allcourseinfo", "coursenum='"+coursenum+"'");
					JSONObject courseinfo=new JSONObject();
					while(rs.next()){
						courseinfo.put("COURSENUM",coursenum);
						courseinfo.put("PHONE", rs.getString("phone")+"");
						courseinfo.put("DEPARTMENT", rs.getString("department")+"");
						courseinfo.put("TEACHER", rs.getString("teaname")+"");
						courseinfo.put("TITLE", rs.getString("title")+"");
					}
					sendjson.put("COURSEINFO", courseinfo);
				}
			}else{
				sendjson.put("STATUS", false);
				return;
			}
			rs = sql.select("stu_choice_begin_date,stu_choice_end_date", "flag_date","depart_no="+departnum);
			if(rs.next()){
				String time="",title="";
				try {
					if(now.compareTo(format.parse(time=rs.getString("stu_choice_begin_date")))<0)title="CHOICEBEGIN";
					else if(now.compareTo(format.parse(time=rs.getString("stu_choice_end_date")))<0)title="CHOICEEND";
					else {
						time="0";
						title="END";
					}
					sendjson.put("TIMETITLE", title);
					sendjson.put("TIME", time);
				} catch (ParseException e) {
					System.out.println("时间格式错误");
				}
			}else {
				sendjson.put("STATUS", false);
				return;
			}
			sendjson.put("STATUS",true);
		}else sendjson.put("STATUS", false);
	}

	public String getJsonStr(){
		return sendjson.toString();
	}
}
