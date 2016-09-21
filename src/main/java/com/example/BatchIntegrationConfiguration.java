package com.example;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.repository.support.MapJobRepositoryFactoryBean;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.file.filters.AcceptOnceFileListFilter;
import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.filters.IgnoreHiddenFileListFilter;
import org.springframework.integration.file.filters.LastModifiedFileListFilter;
import org.springframework.integration.file.filters.RegexPatternFileListFilter;
import org.springframework.integration.file.locking.NioFileLocker;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Component;


@Configuration
@EnableIntegration
@EnableBatchProcessing
@Component
@PropertySource("classpath:batchload-${spring.profiles.active}.properties")
public class BatchIntegrationConfiguration {
	private static final Logger logger = LoggerFactory.getLogger(BatchIntegrationConfiguration.class);
	

	
    @Value("${batch.inbound.directory}")
    private String pollingDirectory;
    
    @Value("${batch.processed.directory}")
    private String processedDirectory;
    
    @Value("${batch.error.directory}")
    private String errorDirectory;
    
    @Autowired
    DataSource datasource;

    @Bean
    public MessageChannel claimInputChannel() {
        return new DirectChannel();
    }

	@Bean
    @InboundChannelAdapter(value = "claimInputChannel", poller = @Poller(fixedDelay = "5000"))
    public FileReadingMessageSource claimPollingFileSource(){
    	logger.debug("Setting up inbound channel adapter ===> claimInputChannel");
    	
    	CompositeFileListFilter<File> compositeFileListFilter= new CompositeFileListFilter<File>();
    	compositeFileListFilter.addFilter(new RegexPatternFileListFilter("(?i).*_CLAIM_.*[.]txt"));
    	// compositeFileListFilter.addFilter(new AcceptOnceFileListFilter<File>()); // uncomment this and nothing seems to work; leaving for later research
    	compositeFileListFilter.addFilter(lastModifiedFilter());
    	compositeFileListFilter.addFilter(new IgnoreHiddenFileListFilter());


    	FileReadingMessageSource pollDirectory = new FileReadingMessageSource();
    	
    	pollDirectory.setDirectory(new File(pollingDirectory)); // hidden files are automatically ignored by default but acts differently between unix and windows
    	pollDirectory.setAutoCreateDirectory(false); //TOD: handle exception when directory does not exist
    	pollDirectory.setFilter(compositeFileListFilter);

		pollDirectory.setLocker(new NioFileLocker()); // will acquire a lock before the file is allowed to be received. We have to unlock it later. FileLocker.unlock(File file)
		
		logger.debug("claimInputChannel setup complete");
		
		return pollDirectory;
    }

    // filter which checks to make sure the file is not still being updated.
    @Bean
    public LastModifiedFileListFilter lastModifiedFilter(){    	
    	logger.debug("Setting up inbound claim channel adapter ===> lastModifiedFilter for claim channel");
    	LastModifiedFileListFilter fileFilter = new LastModifiedFileListFilter();  
    	fileFilter.setAge(15);
		return fileFilter;
    }
    
    @Transformer(inputChannel = "claimInputChannel", outputChannel = "nullChannel")  
    public String messageToRequest(Message<File> message) {
    	logger.debug("=======> converting message to a request <=======");
    	logger.debug("=======>             CLAIM               <=======");

    	
        JobParametersBuilder jobParametersBuilder = new JobParametersBuilder();
        jobParametersBuilder.addString("fileName", message.getPayload().getAbsolutePath());
        logger.debug("Job Launching with filename: " + message.getPayload().getAbsolutePath());
        logger.debug("Job Paramaters: " + jobParametersBuilder.toJobParameters());

        
        File dir = new File(processedDirectory);
		if (dir.exists()) {
			// returns the absolute path of the file on the system
			Path sourcePath = Paths.get(message.getPayload().getAbsolutePath());

			// just an easy way to get the filename
			String target = processedDirectory + "/" + sourcePath.getFileName();

			logger.debug("Moving processed file " + sourcePath.toString() + " to " + target);

			try {
				Files.move(sourcePath, Paths.get(target), StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				logger.error("Something went wrong in moving file to processed directory.");
				e.printStackTrace();
			}

		} else {
			logger.error("The " + processedDirectory + " does not exist.");
		}
		
        //JobLaunchRequest request = new JobLaunchRequest(job, jobParametersBuilder.toJobParameters());

		return "Just Testing";
    }

}
