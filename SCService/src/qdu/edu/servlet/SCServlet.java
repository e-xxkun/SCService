package qdu.edu.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/SCServlet")
public class SCServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
    
    public SCServlet() {
        super();
    }

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		request.setCharacterEncoding("utf-8");
		response.setHeader("Content-type", "text/html;charset=UTF-8");
		response.setCharacterEncoding("utf-8");
        System.out.println("service receiver sccess");
        BufferedReader br = new BufferedReader(new InputStreamReader((ServletInputStream) request.getInputStream()));
        StringBuffer sb = new StringBuffer("");
        String temp;
        while ((temp=br.readLine())!=null){
            sb.append(temp);
        }
        br.close();
        System.out.println("请求报文:" + sb.toString());
        System.out.println("请求成功");
        JsonTools jt=new JsonTools(sb.toString());
        PrintWriter pw = response.getWriter();
        pw.write(jt.getJsonStr());
        System.out.println("返回报文:" + jt.getJsonStr());
        System.out.println("返回成功");
        pw.flush();
        pw.close();
	}
}
