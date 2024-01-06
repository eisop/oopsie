CREATE TABLE `log` (
                       `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `dateTime` datetime NOT NULL,
    `provider_no` varchar(10) DEFAULT NULL,
    `action` varchar(100) DEFAULT NULL,
    `content` varchar(80) DEFAULT NULL,
    `contentId` varchar(80) DEFAULT NULL,
    `ip` varchar(64) DEFAULT NULL,
    `demographic_no` int(10) DEFAULT NULL,
    `data` text,
    `securityId` int(11) DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `datetime` (`dateTime`,`provider_no`),
    KEY `action` (`action`),
    KEY `content` (`content`),
    KEY `contentId` (`contentId`),
    KEY `demographic_no` (`demographic_no`),
    KEY `provider_noIndex` (`provider_no`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;