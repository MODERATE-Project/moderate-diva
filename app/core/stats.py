from core.settings import topic_stats, kfg_valid

import logging
logger = logging.getLogger()

def compute_stats():
    """It computes the statistics for each message received by Kafka.
    The consumers of each topic returns a message that is then passed to the `update` method
    of the `TopicStats` object.

    """

    global topic_stats

    while True:
        messages = kfg_valid.receive()
        for message in messages:
            topic_stats.update(message)