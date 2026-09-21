#!/usr/bin/env python3
"""Prueba real REST + Redis. Usar únicamente en un entorno local de pruebas."""
import json, os, time, urllib.request, urllib.error, concurrent.futures
BASE=os.environ.get("BACKEND_URL","http://localhost:8080")
def api(ruta,metodo="GET",datos=None,token=None,esperado=200):
    cabeceras={"Content-Type":"application/json"}
    if token:cabeceras["X-Token"]=token
    solicitud=urllib.request.Request(BASE+ruta,data=None if datos is None else json.dumps(datos).encode(),headers=cabeceras,method=metodo)
    try:
        with urllib.request.urlopen(solicitud,timeout=30) as r: estado,cuerpo=r.status,r.read()
    except urllib.error.HTTPError as e:estado,cuerpo=e.code,e.read()
    assert estado==esperado,(ruta,estado,cuerpo.decode())
    return json.loads(cuerpo) if cuerpo else None
conceptos=[{"palabra":s,"definicion":"Pista de prueba "+str(i)} for i,s in enumerate(["CONTENEDOR","IMAGEN","DOCKER","VOLUMEN","PUERTO","RED","SERVICIO","REGISTRO","CAPA","COMPOSE","PROCESO","SISTEMA","TERMINAL"])]
def ejecutar():
    assert api("/actuator/health")["status"]=="UP"
    p=api("/api/partidas","POST",dict(nombre="Integración",materia="Docker",nombreProfesor="Docente",correoProfesor="profesor@example.test",idioma="es",duracionMinutos=5,mostrarResultados=False,intentosPorPalabra=2,cantidadPalabrasSolicitada=5,conceptos=conceptos))
    ruta="/api/partidas/"+p["idPartida"];token=p["token"]
    try:
        j=api(ruta+"/jugadores","POST",dict(nombre="Ana",correo="ana@example.test"));jr=ruta+"/jugadores/"+j["idJugador"]
        api(ruta+"/finalizar","POST",token=j["token"],esperado=403)
        api(ruta,"GET",token=j["token"],esperado=403)
        api(ruta+"/ranking",token=j["token"],esperado=403)
        api(ruta+"/iniciar","POST",token=token)
        v=api(jr+"/heartbeat","POST",dict(pantallaActiva=False),j["token"])
        raw=json.dumps(v)
        tablero=api(ruta,token=token)["crucigrama"]
        assert all(palabra["palabra"] not in raw for palabra in tablero["palabras"])
        assert not any(s in raw for s in ["segundosRestantes","fechaHoraFin","hashToken","correoProfesor"])
        assert api(ruta,token=token)["jugadores"][0]["pantallaActiva"] is False
        time.sleep(3.2)
        for palabra in tablero["palabras"]:
            resultado=api(jr+"/respuestas","POST",dict(idPalabra=palabra["idPalabra"],respuesta=palabra["palabra"]),j["token"])
            assert resultado["correcta"]
        final=api(ruta,token=token)
        assert final["estado"]=="FINALIZADA" and final["ranking"][0]["puntaje"]==100
        api(ruta+"/ranking",token=j["token"],esperado=403)
        time.sleep(5.2)
        assert "ERROR" in api(ruta,token=token)["correos"]
        api(ruta+"/correos/reintentar","POST",token=token)
        print("OK: Redis, REST, autorización, DTO sin soluciones, visibilidad, puntaje, cierre y fallo SMTP.")
    finally:api(ruta,"DELETE",token=token)
    p=api("/api/partidas","POST",dict(nombre="Capacidad",materia="Docker",nombreProfesor="Docente",correoProfesor="profesor@example.test",idioma="es",duracionMinutos=5,mostrarResultados=True,intentosPorPalabra=0,cantidadPalabrasSolicitada=5,conceptos=conceptos));ruta="/api/partidas/"+p["idPartida"]
    try:
        inicio=time.monotonic()
        with concurrent.futures.ThreadPoolExecutor(max_workers=20) as grupo:
            alumnos=list(grupo.map(lambda i:api(ruta+"/jugadores","POST",dict(nombre="Alumno "+str(i),correo=f"a{i}@example.test")),range(60)))
        assert api(ruta,token=p["token"])["cantidadJugadores"]==60
        api(ruta+"/jugadores","POST",dict(nombre="Extra",correo="extra@example.test"),esperado=400)
        with concurrent.futures.ThreadPoolExecutor(max_workers=20) as grupo:
            list(grupo.map(lambda j:api(ruta+"/jugadores/"+j["idJugador"]+"/heartbeat","POST",dict(pantallaActiva=True),j["token"]),alumnos))
        print(f"OK: 60 registros y 60 heartbeats concurrentes en {time.monotonic()-inicio:.2f} s; participante 61 rechazado.")
    finally:api(ruta,"DELETE",token=p["token"])
if __name__=="__main__":ejecutar()
