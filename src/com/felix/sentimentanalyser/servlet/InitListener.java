package com.felix.sentimentanalyser.servlet;

import java.util.Locale;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.felix.sentimentanalyser.GateWrapper;
import com.felix.sentimentanalyser.util.Constants;
import com.felix.util.FileUtil;
import com.felix.util.KeyValues;
import com.felix.util.logging.Log4JLogger;
import com.felix.util.logging.LoggerInterface;
import com.felix.webmaintenance.GlobalConfig;
import com.felix.webmaintenance.MaintainedServer;
import com.felix.webmaintenance.MaintenanceManager;

public class InitListener implements ServletContextListener, MaintainedServer {
	private ServletContext _servletContext;
	private LoggerInterface _logger = null;
	private KeyValues _config = null;
	private GateWrapper _gateWrapper;

	public InitListener() {
	}

	public void contextInitialized(ServletContextEvent sce) {
		_servletContext = sce.getServletContext();
		init();
	}

	private void init() {
		Locale locale = Locale.GERMAN;
		try {
			// boolean withRss =
			String configFile = _servletContext.getInitParameter("configFile");
			String rootPath = _servletContext.getRealPath("/");
			System.setProperty("rootPath", rootPath);
			String appName = _servletContext.getServletContextName();
			_servletContext.setAttribute("appName", appName);
			GlobalConfig global = new GlobalConfig(_servletContext, configFile);
			_logger = (LoggerInterface) new Log4JLogger(global.getLogger().getName());
			_servletContext.setAttribute("logger", _logger);
			global.getLogger().debug("Starting " + _servletContext.getServletContextName() + " Server.");
			_config = GlobalConfig.getInstance();
			// _servletContext.setAttribute("logger", logger);
			_servletContext.setAttribute("config", _config);
			_servletContext.setAttribute("locale", locale);

			_gateWrapper = new GateWrapper(_logger, _config);
			_gateWrapper.initApplication(_config.getString("gateAppPath"));
			_servletContext.setAttribute("gateWrapper", _gateWrapper);

			MaintenanceManager maintenanceManger = new MaintenanceManager(global.getConfFilePath(), rootPath, _logger);
			maintenanceManger.setPreproFilter(global.getPathValue(MaintenanceManager.PREPRO_PATH), null);
			maintenanceManger.parseConfig();
			maintenanceManger.setMaintainedServer(this);
			_servletContext.setAttribute("maintenanceManager", maintenanceManger);

			System.out.println("Server " + _servletContext.getServletContextName() + ", version " + Constants.Version
					+ " started");
			System.out.println("with base " + rootPath);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
	}

	@Override
	public void reInitializeServer() {
		init();
	}

	@Override
	public void executeCommand(String command) {
		if (command.compareTo("exec reinit prs") == 0) {
			String fn = _config.getPathValue("sentimentGazetteerFile");
			try {
				FileUtil.delete(fn);
			} catch (Exception e) {
				_logger.error(e.getMessage());
				e.printStackTrace();
			}
			_gateWrapper.reInitProcessingResources();
		}
	}

	@Override
	public String showInfo(String infoDescriptor) {
		return "version: " + Constants.Version;
	}

}
