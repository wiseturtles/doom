package com.doom.mvc.controller;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javassist.ClassPool;
import javassist.NotFoundException;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.LocalVariableAttribute;
import javassist.bytecode.MethodInfo;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.doom.mvc.annotation.Handler;
import com.doom.mvc.annotation.Response;
import com.doom.mvc.utils.ClassUtils;

/**
 * @author yuzhang <z99370324@gmail.com>
 * 
 */

public class DispatcherServlet extends HttpServlet {

	/**
     * 
     */
	private static final long serialVersionUID = 1L;

	private Map<String, Method> requestMethodMap = new HashMap<String, Method>();

	@Override
	public void init(ServletConfig config) throws ServletException {
		// TODO 通过配置需要扫描的包
		Set<Class<?>> classSet = ClassUtils.getClasses("com");
		ServletContext ctx = getServletContext();
		ctx.setAttribute("classPool", ClassPool.getDefault());
		ctx.setAttribute("router", requestMethodMap);
		for (Class<?> clazz : classSet) {
			try {
				Handler handlerAnnotation = clazz.getAnnotation(Handler.class);
				// 判定类是否带有Handler注解
				if (handlerAnnotation != null) {
					Method[] methods = clazz.getDeclaredMethods();
					for (Method method : methods) {

						Response responseAnnotation = method
								.getAnnotation(Response.class);
						// 判定该类的方法是否带有Response注解
						if (responseAnnotation != null) {
							// 不能将service bean注册为non-public or static
							int modifyMask = method.getModifiers();
							if (!Modifier.isPublic(modifyMask)
									|| Modifier.isStatic(modifyMask))
								throw new Exception(
										"service bean must be registered as static or non-public.");
							String path = handlerAnnotation.path()
									+ responseAnnotation.path();
							// map中若已经存在一个相同的路径则抛出异常
							if (requestMethodMap.containsKey(path))
								throw new Exception(
										"identical url detected between "
												+ clazz.getName()
												+ " and "
												+ requestMethodMap.get(path)
														.getDeclaringClass()
														.getName());
							// 记录请求路径指向注册的方法
							requestMethodMap.put(path, method);
							// 全局生成一个该方法的实例对象,该类不能只定义带参数的构造方法
							ctx.setAttribute(clazz.getName(),
									clazz.newInstance());

						}
					}
				}
			} catch (Exception e) {
				// TODO: handle exception
			}
		}
	}

	@Override
	public void service(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// this.request = request;
		// this.response = response;
		// this.session = request.getSession();
		// 获取请求路径
		Method realHandlerMethod = requestMethodMap
				.get(request.getRequestURI());
		String realHandlerName = realHandlerMethod.getDeclaringClass()
				.getName();
		Class<?>[] paramTypes = realHandlerMethod.getParameterTypes();
		Object[] values = new Object[paramTypes.length];
		Map<String, String[]> paramsMap = request.getParameterMap();

		try {
			// attr.variableName(0);
			ServletContext ctx = getServletContext();
			ClassPool classPool = (ClassPool) ctx.getAttribute("classPool");
			MethodInfo methodInfo = classPool.getMethod(realHandlerName,
					realHandlerMethod.getName()).getMethodInfo();
			CodeAttribute codeAttribute = methodInfo.getCodeAttribute();
			LocalVariableAttribute attr = (LocalVariableAttribute) codeAttribute
					.getAttribute(LocalVariableAttribute.tag);
			for (int i = 0; i < values.length; i++) {
				if (paramTypes[i].equals(HttpSession.class))
					values[i] = request.getSession();
				else if (paramTypes[i].equals(HttpServletRequest.class)
						|| paramTypes[i].equals(HttpServletRequest.class
								.getGenericSuperclass()))
					values[i] = request;
				else if (paramTypes[i].equals(HttpServletResponse.class)
						|| paramTypes.equals(HttpServletResponse.class
								.getGenericSuperclass()))
					values[i] = response;
				else if (paramTypes[i].isArray())
					values[i] = paramsMap.get(attr.variableName(i));
				else
					values[i] = paramsMap.get(attr.variableName(i))[0];
			}
			realHandlerMethod.invoke(ctx.getAttribute(realHandlerName), values);
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
