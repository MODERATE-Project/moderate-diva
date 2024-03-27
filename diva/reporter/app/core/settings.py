from pydantic import BaseSettings, List
from sqlalchemy import create_engine, Column, Integer, String, ForeignKey, MetaData
from sqlalchemy.sql import func
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker, Session
from pydoc import importfile

from core.kfg import KafkaCommunicationGateway

import os

class Settings(BaseSettings):
    """It contains the parameters used by the backend.
    They are then explicitly adopted by the single modules.

    Args:
        BaseSettings: `pydantic` class 
    """

    app_name: str = "MODERATE"
    app_description: str = "MODERATE Access Point for Quality Report"
    version: str = "1.0.0"
    crt: str = "config/localhost.crt"
    key: str = "config/localhost.key"
    config_path: str = "config/frontend.json"
    broker = os.getenv("KAFKA_BOOTSTRAP_SERVERS") or "localhost:9092"
    security_protocol: str = "SASL_SSL"
    mechanism: str = "PLAIN"
    sasl_username: str = os.getenv("KAFKA_SASL_USERNAME") or "kafka-dq"
    sasl_password: str = os.getenv("KAFKA_SASL_PASSWORD") or "KAFKA!dq!2023"
    validation_topic: str = os.getenv("VALIDATION_TOPIC") or "validation"
    host: str = "localhost"
    port: int = 8000
    api_keys: str = (os.getenv("API_KEYS").split(",") if "," in os.getenv("API_KEYS") else os.getenv("API_KEYS")) or ["ABC12345"]
    response_404: dict = {"description": "Not found"}
    database: str = os.getenv("DATABASE_PATH") or "sqlite:///./report.db"

class TopicStats():
    """It stores the statistics computed on the messages read by Kafka.
    When a new message available, the backend computes the GUI 
    statistics and the results are stored inside this class.
    """

    def __init__(self, settings: Settings):
        """Initialization of all statistic variables.
        """
        self.reporter = {}
        self.engine = create_engine(settings.database)
        self.metadata = MetaData()
        self.session = sessionmaker(autocommit=False, autoflush=False, bind=self.engine)
        self.Base = declarative_base(metadata=self.metadata)

        class Report(self.Base):
            __tablename__ = "report"
            validator = Column(String, primary_key=True)
            type = Column(String, primary_key=True)
            feature = Column(String, primary_key=True)
            valid = Column(Integer, default=0)
            fail = Column(Integer, default=0)
        
            def as_dict(self):
                return {column.name: getattr(self, column.name) for column in self.__table__.columns}
            
        self.Report = Report

        self.Base.metadata.create_all(bind=self.engine)


    def update(self, message: dict):
        """It updates the information about a simulation or ai request.

        Args:
            message (dict): message read by the backend.
            simulation (bool): `true` if a simulation message is received, `false` otherwise.
        """

        validator, type, feature, result = message["message"]["validator"], message["message"]["type"], message["message"]["feature"], message["message"]["result"]
        with Session(self.engine) as db:
            report = db.query(self.Report).filter(self.Report.validator == validator and self.Report.type == type and self.Report.feature == feature).first()
            if report is None:
                report = self.Report()
                report.validator = validator
                report.type = type
                report.feature = feature
                if result == "VALID":
                    report.valid = 1
                else:
                    report.fail = 1
                
                db.add(report)
            else:
                if result == "VALID":
                    report.valid += 1
                else:
                    report.fail += 1

                
            db.commit()

    def get_report(self):
        """It returns the information about all the simulation requests read by the backend.

        Returns:
            dict: map containing the information about all the simulation request executed by the user.
        """

        with Session(self.engine) as db:
            group_by_keys = [self.Report.validator, self.Report.type, self.Report.feature]
            items = db.query(*group_by_keys, func.sum(self.Report.valid).label("valid"), func.sum(self.Report.fail).label("fail")).group_by(self.Report.validator, self.Report.type, self.Report.feature).all()
            ret = {}
            for validator, type, feature, valid, fail in items:
                if validator not in ret:
                    ret[validator] = {}

                if type not in ret[validator]:
                    ret[validator][type] = {}


                ret[validator][type][feature] = {
                    "VALID": valid,
                    "FAIL": fail
                }

            return ret

settings = Settings()

if os.getenv("CONFIG_LOCATION") is not None:
    import configparser
    config = configparser.RawConfigParser()
    config.read(os.getenv("CONFIG_LOCATION"))
    
    config_dict = dict(config.items('backend'))
    settings.validation_topic = config_dict["validation.topic"]

# A single KafkaCommunicationGateway is instanced for the validation topic
kfg_valid = KafkaCommunicationGateway(os.getenv("HOSTNAME"), settings.validation_topic)
topic_stats = TopicStats(settings)