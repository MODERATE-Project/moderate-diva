from core.settings import settings

"""This method verifies if the API KEY provided by the user is correct

Returns:
    bool: True if the user is authenticated, False otherwise
"""
def check_token(token: str):
    return token in settings.api_keys

"""This method verifies if the API KEY provided by the user is correct

Returns:
    bool: True if the user is authenticated, False otherwise
"""
def get_user(token: str):
    if check_token(token):
        return "admin"