package qdu.edu.servlet;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SqlTools {

	private static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    private static final String DB_URL = "jdbc:mysql://localhost:3306/choicedb";
    private static final String USEID = "root";
    private static final String PASSWD = "xkk0512";
    private Connection connection;
	private PreparedStatement statement;
	private String sql;
	
	public SqlTools() throws ClassNotFoundException, SQLException{
		connection();
	}
	
	private void connection() throws ClassNotFoundException, SQLException{
		Class.forName(JDBC_DRIVER);
		connection = DriverManager.getConnection(DB_URL,USEID,PASSWD);
		if(connection.isClosed())System.out.println("数据库连接失败");
		else System.out.println("数据库连接成功");
	}
	
	public ResultSet select(String select,String from,String where) throws SQLException{
		sql="select "+select+" from "+from+" where "+where;
		statement=connection.prepareStatement(sql);
		return statement.executeQuery();
	}
	
	public int update(String update,String set,String where) throws SQLException{
		sql="update "+update+" set "+set+" where "+where;
		statement=connection.prepareStatement(sql);
		return statement.executeUpdate();
	}
	
	public int insert(String into,String zd,String values) throws SQLException{
		sql="insert into "+into+"("+zd+") values("+values+")";
		statement=connection.prepareStatement(sql);
		return statement.executeUpdate();
	}
	
	public int delete(String from,String where) throws SQLException {
		sql="delete from "+from+" where "+where;
		statement=connection.prepareStatement(sql);
		return statement.executeUpdate();
	}
}
