import requests
import json
import jsonpath
def ${functionName}(**kwargs):
    url =  "${url}"
    headers =  ${headers}
    # Below params will be replaced by llmChat automatically
    params = ${requestBody}

    response_type = "mapping"
    response_handle = ${responseHandle}
    default_error_msg = "failed to Request page"

    def request(url, params):
        response = None
        try:
            response = requests.post(url, data=json.dumps(params), headers=headers)
        except Exception as e:
            print("Fail to get response from webhook. ", e)
        return response

    def __interpolator_text(response, states):
        import re
        try:
            text = re.sub(r"\${([^\n{}]+?)}", r"{0[\1]}", response)
            text = text.format(states)
            if "0[" in text:
                # regex replaced tag but format did not replace
                # likely cause would be that tag name was enclosed
                # in double curly and format func simply escaped it.
                # we don't want to return {0[SLOTNAME]} thus
                # restoring original value with { being escaped.
                return response.format({})
            return text
        except Exception as e:
            print("cannot find states", e)
            return response

    def parse(json_dict, value):
        return jsonpath.jsonpath(json_dict, value)

    for key, value in params.items():
        params[key] = __interpolator_text(value, kwargs)
    response = request(url, params)
    result = []

    if response is not None and response.status_code == 200:
        text = response_handle.get("text")
        tmp_slots = kwargs
        for item in response_handle.get("parse"):
            key = item.get("key")
            value = item.get("value")
            mapping = parse(response.json(), value)
            if not mapping:
                break
            if len(mapping) == 0:
                continue
            result.append({"slot_name": key, "value": mapping[0] if len(mapping) == 1 else mapping})
            tmp_slots[key] = mapping[0] if len(mapping) == 1 else mapping

    return result
