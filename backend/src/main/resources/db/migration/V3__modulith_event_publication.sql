-- Spring Modulith JDBC event registry for MySQL. Completion is archived.
CREATE TABLE EVENT_PUBLICATION (
  id VARCHAR(36) NOT NULL, listener_id VARCHAR(512) NOT NULL, event_type VARCHAR(512) NOT NULL,
  serialized_event VARCHAR(4000) NOT NULL, publication_date TIMESTAMP(6) NOT NULL,
  completion_date TIMESTAMP(6) NULL, PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX event_publication_by_completion_date_idx ON EVENT_PUBLICATION (completion_date);
CREATE TABLE EVENT_PUBLICATION_ARCHIVE (
  id VARCHAR(36) NOT NULL, listener_id VARCHAR(512) NOT NULL, event_type VARCHAR(512) NOT NULL,
  serialized_event VARCHAR(4000) NOT NULL, publication_date TIMESTAMP(6) NOT NULL,
  completion_date TIMESTAMP(6) NULL, PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX event_publication_archive_by_completion_date_idx ON EVENT_PUBLICATION_ARCHIVE (completion_date);
