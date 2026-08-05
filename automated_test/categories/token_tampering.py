"""Category 5: Token tampering -- flip JWT claims WITHOUT re-signing, using
only base64 manipulation (no signing key needed). The server MUST reject
every variant; a 2xx response means signature verification is broken.
"""
import base64
import json
from common import req, rec, auth_header


def _b64url_decode(seg):
    seg += "=" * (-len(seg) % 4)
    return base64.urlsafe_b64decode(seg.encode())


def _b64url_encode(data: bytes):
    return base64.urlsafe_b64encode(data).rstrip(b"=").decode()


def _tamper_claim(token, claim, new_value):
    header_b64, payload_b64, sig_b64 = token.split(".")
    payload = json.loads(_b64url_decode(payload_b64))
    payload[claim] = new_value
    new_payload_b64 = _b64url_encode(json.dumps(payload).encode())
    # keep the ORIGINAL signature -- it will no longer match the tampered payload
    return f"{header_b64}.{new_payload_b64}.{sig_b64}"


def _alg_none_token(token, claim=None, new_value=None):
    header_b64, payload_b64, _sig_b64 = token.split(".")
    header = json.loads(_b64url_decode(header_b64))
    header["alg"] = "none"
    payload = json.loads(_b64url_decode(payload_b64))
    if claim:
        payload[claim] = new_value
    new_header_b64 = _b64url_encode(json.dumps(header).encode())
    new_payload_b64 = _b64url_encode(json.dumps(payload).encode())
    return f"{new_header_b64}.{new_payload_b64}."  # empty signature segment


def _garbage_sig_token(token):
    header_b64, payload_b64, _sig_b64 = token.split(".")
    return f"{header_b64}.{payload_b64}.dGFtcGVyZWRzaWduYXR1cmU"


def run(tokens, fixtures):
    results = []
    base_token = tokens.get("patientA")
    doctor_email = "dr.heather.lin@secureehr.test"

    if not base_token:
        return [rec("/auth/me", "GET", "tampered", None, "401", False, "info", None, "token_tampering", "Skipped -- no base token available to tamper with")]

    variants = {
        "role/sub flipped, original signature kept (should fail sig check)": _tamper_claim(base_token, "sub", doctor_email),
        "alg=none with sub flipped to doctor, empty signature": _alg_none_token(base_token, "sub", doctor_email),
        "alg=none, claims unchanged, empty signature": _alg_none_token(base_token),
        "original payload, garbage/random signature": _garbage_sig_token(base_token),
    }

    targets = [("GET", "/auth/me"), ("GET", "/doctor/profile"), ("GET", "/patients", )]

    for label, tampered in variants.items():
        for method, path in targets:
            status, elapsed, _r = req(method, path, headers={"Authorization": f"Bearer {tampered}"})
            finding = status is not None and 200 <= status < 300
            results.append(rec(
                path, method, "tampered", status, "401", finding,
                "critical" if finding else "info", elapsed, "token_tampering",
                f"{label} -- " + ("SIGNATURE NOT VERIFIED, tampered token accepted!" if finding else "correctly rejected"),
            ))

    return results
